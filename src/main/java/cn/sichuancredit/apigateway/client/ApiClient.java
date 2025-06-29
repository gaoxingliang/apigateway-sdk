package cn.sichuancredit.apigateway.client;

import cn.hutool.core.util.*;
import cn.sichuancredit.apigateway.encryption.*;
import com.alibaba.fastjson2.*;
import kong.unirest.*;
import kong.unirest.HttpRequest;
import kong.unirest.HttpResponse;
import org.apache.commons.lang3.*;
import org.apache.commons.logging.*;
import org.apache.http.*;

import java.io.*;
import java.util.*;

public class ApiClient {
    public static final String HEADER_FORM_ENCRYPTED_FIELDS = "form-encrypted-fields";
    public static final String HEADER_FORM_ENCRYPTED_FIELDS_NONE = "none";
    private static final Log log = LogFactory.getLog(ApiClient.class);

    private final UnirestInstance instance;
    private final ApiConfig apiConfig;
    private volatile String token;
    private volatile long tokenCreatedTime;

    public ApiClient(ApiConfig apiConfig) {
        this.apiConfig = apiConfig;
        Config config = new Config();
        if (apiConfig.connectTimeoutInSeconds > 0) {
            config.connectTimeout(apiConfig.connectTimeoutInSeconds * 1000);
        }
        if (apiConfig.readTimeoutInSeconds > 0) {
            config.socketTimeout(apiConfig.readTimeoutInSeconds * 1000);
        }
        if (apiConfig.proxyHost != null) {
            config.proxy(apiConfig.proxyHost, apiConfig.proxyPort, apiConfig.username, apiConfig.password);
        }
        config.automaticRetries(apiConfig.automaticRetries);
        config.verifySsl(false);
        instance = new UnirestInstance(config);
    }

    private synchronized void createTokenIfNeeded() {
        if (token == null || (apiConfig.tokenExpireTimeInSeconds > 0 && System.currentTimeMillis() - tokenCreatedTime > apiConfig.tokenExpireTimeInSeconds * 1000)) {
            JSONObject d = new JSONObject();
            d.put("appid", apiConfig.app);
            d.put("username", apiConfig.username);
            d.put("password", apiConfig.password);

            HttpRequest getTokenRequest = instance.post(apiConfig.url + "/auth/token")
                    .header("Content-Type", "application/json").body(d.toString());
            HttpResponse<String> response = _sendHttpRequest(getTokenRequest);

            JSONObject json = JSONObject.parseObject(response.getBody());
            String t = json.getString("rbac_token");
            if (t == null) {
                throw new ApiException("未找到token：" + response.getBody());
            }
            token = t;
            tokenCreatedTime = System.currentTimeMillis();
        }
    }

    private void checkResponse(HttpResponse<String> response, String message) {
        if (response.getStatus() != 200 || !response.isSuccess()) {
            throw generateApiException(response, message);
        }
    }

    private ApiException generateApiException(HttpResponse<String> response, String message) {
        return new ApiException(message).setResponseCode(response.getStatus())
                .setResponseBody(response.getBody())
                .setResponseHeaders(response.getHeaders());
    }

    public String postJson(String path, String json, Map<String, String> headers, boolean needDecryption) {
        createTokenIfNeeded();
        String finalBody = json;

        if (json != null && json.length() > 0) {
            if (needDecryption) {
                String key = MySmUtil.generateSm4Key();
                String encryptKey = MySmUtil.sm2Encrypt(key, apiConfig.publicKey);
                String data = MySmUtil.sm4Encrypt(json, key);
                JSONObject encryptedData = new JSONObject();
                encryptedData.put("encryptKey", encryptKey);
                encryptedData.put("data", data);
                String encryptedJson = encryptedData.toJSONString();
                finalBody = encryptedJson;
            }
        }
        RequestBodyEntity req = instance.post(apiConfig.url + path).header(HttpHeaders.AUTHORIZATION, token)
                .contentType(ContentType.APPLICATION_JSON.getMimeType())
                .body(finalBody);

        if (headers != null) {
            req = req.headers(headers);
        }

        return sendOutRequest(needDecryption, req);
    }

    public String postJson(String path, String json, boolean needDecryption) {
        return postJson(path, json, null, needDecryption);
    }

    public String get(String path, Map<String, Object> queryParameters, Map<String, String> headers, boolean needDecryption) {
        createTokenIfNeeded();
        GetRequest request = instance.get(apiConfig.url + path)
                .header(HttpHeaders.AUTHORIZATION, token);
        if (queryParameters != null) {
            queryParameters.forEach((k, v) -> {
                if (v instanceof Collection) {
                    request.queryString(k, (Collection) v);
                } else {
                    request.queryString(k, v);
                }
            });
        }

        if (headers != null) {
            request.headers(headers);
        }

        return sendOutRequest(needDecryption, request);
    }

    /**
     * form参数，form中的值. 目前未对form中的参数进行加密
     * form中添加文件可以：form.put("file", new File("xxx"));
     * @param path
     * @param form
     * @return
     */
    public String postForm(String path, Map<String, String> headers, Map<String, Object> form) {
        createTokenIfNeeded();
        HttpRequestWithBody req = instance.post(apiConfig.url + path).header(HttpHeaders.AUTHORIZATION, token);
        if (headers != null) {
            req.headers(headers);
        }
        HttpRequest finalReq = req;
        if (form != null) {
            finalReq = req.fields(form).header(HEADER_FORM_ENCRYPTED_FIELDS, HEADER_FORM_ENCRYPTED_FIELDS_NONE);
        }

        return sendOutRequest(true, finalReq);
    }

    public String get(String path, Map<String, Object> queryParameters, boolean needDecryption) {
        return get(path, queryParameters, null, needDecryption);
    }

    private HttpResponse<String> _sendHttpRequest(HttpRequest request) {
        HttpResponse<String> response = null;
        int retryTimes = 3;
        for (int i = 0; i < retryTimes; i++) {
            try {
                response = request.asString();
            } catch (Exception e) {
                throw new ApiException("请求失败", e);
            }

            // 有时候wolf不太稳定，所以这里在retry=true的时候 自动重试下
            try {
                checkResponse(response, "请求处理失败");
                break;
            } catch (ApiException e) {
                if (apiConfig.retryHttpHandler != null && apiConfig.retryHttpHandler.retry(response.getStatus(), response.getBody())) {
                    if (i >= retryTimes - 1) {
                        log.error("重试后，依然失败");
                        throw e;
                    }
                    try {
                        log.warn("等待重试请求，当前重试次数：" + i);
                        Thread.sleep(5000);
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                        throw new ApiException("线程中断，退出", e);
                    }
                } else {
                    throw e;
                }
            }
        }

        return response;
    }

    private String sendOutRequest(boolean needDecryption, HttpRequest request) {
        HttpResponse<String> response = _sendHttpRequest(request);
        String result = response.getBody();
        if (needDecryption) {
            EncryptedData responseEncryptedData = JSONObject.parseObject(response.getBody(), EncryptedData.class);
            try {
                String sm4Key = MySmUtil.sm2Decrypt(responseEncryptedData.getEncryptKey(), apiConfig.privateKey);
                result = MySmUtil.sm4Decrypt(responseEncryptedData.getData(), sm4Key);
            } catch (Exception e) {
                throw generateApiException(response, "解密失败");
            }
        }
        // 需要解压缩的场景
        String zipVersion = response.getHeaders().getFirst("internal-zip-version");
        if (StringUtils.isNotEmpty(zipVersion) && Integer.valueOf(zipVersion) == 1) {
            try {
                result = new String(ZipUtil.unGzip(Base64.getDecoder().decode(result)), "UTF-8");
            } catch (UnsupportedEncodingException e) {
                throw new IllegalArgumentException(e);
            }
        }

        return result;
    }
}
