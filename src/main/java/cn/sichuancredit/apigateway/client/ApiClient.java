package cn.sichuancredit.apigateway.client;

import cn.hutool.core.util.*;
import cn.sichuancredit.apigateway.encryption.*;
import com.alibaba.fastjson2.*;
import kong.unirest.HttpResponse;
import kong.unirest.*;
import org.apache.commons.lang3.*;
import org.apache.http.*;

import java.io.*;
import java.util.*;

public class ApiClient {
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
        instance = new UnirestInstance(config);
    }

    private synchronized void createTokenIfNeeded() {
        if (token == null || (apiConfig.tokenExpireTimeInSeconds > 0 && System.currentTimeMillis() - tokenCreatedTime > apiConfig.tokenExpireTimeInSeconds * 1000)) {
            JSONObject d = new JSONObject();
            d.put("appid", "data");
            d.put("username", apiConfig.username);
            d.put("password", apiConfig.password);
            HttpResponse<String> response;
            try {
                response = instance.post(apiConfig.url + "/auth/token")
                        .header("Content-Type", "application/json")
                        .body(d.toString())
                        .asString();
            } catch (Exception e) {
                throw new ApiException("获取token失败", e);
            }
            checkResponse(response);
            JSONObject json = JSONObject.parseObject(response.getBody());
            String t = json.getString("rbac_token");
            if (t == null) {
                throw new ApiException("未找到token：" + response.getBody());
            }
            token = t;
            tokenCreatedTime = System.currentTimeMillis();
        }
    }

    private void checkResponse(HttpResponse<String> response) {
        if (response.getStatus() != 200 || !response.isSuccess()) {
            throw new ApiException("请求失败:" + response.getStatus() + " 消息体:" + response.getBody() + " 消息头：" + response.getHeaders());
        }
    }

    public String get(String path, Map<String, Object> queryParameters, boolean needDecryption) {
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
        HttpResponse<String> response;
        try {
            response = request.asString();
        } catch (Exception e) {
            throw new ApiException("请求失败", e);
        }
        checkResponse(response);

        String result = response.getBody();
        if (needDecryption) {
            EncryptedData responseEncryptedData = JSONObject.parseObject(response.getBody(), EncryptedData.class);
            try {
                String sm4Key = MySmUtil.sm2Decrypt(responseEncryptedData.getEncryptKey(), apiConfig.privateKey);
                result = MySmUtil.sm4Decrypt(responseEncryptedData.getData(), sm4Key);
            } catch (Exception e) {
                throw new ApiException("解密失败:" + response.getStatus() + " 消息体:" + response.getBody() + " 消息头：" + response.getHeaders(), e);
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
