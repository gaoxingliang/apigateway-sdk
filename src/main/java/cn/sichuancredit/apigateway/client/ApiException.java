package cn.sichuancredit.apigateway.client;

import kong.unirest.*;
import lombok.*;

import javax.annotation.*;
import java.util.*;

public class ApiException extends RuntimeException {
    public ApiException(String message) {
        super(message);
    }

    public ApiException(String message, Exception e) {
        super(message, e);
    }

    /**
     * 返回的response数据
     */
    @Getter
    @Nullable
    private String responseBody;

    /**
     * 返回的http代码
     */
    @Getter
    @Nullable
    private Integer responseCode;

    @Nullable
    @Getter
    private Map<String, Object> responseHeaders;

    public ApiException setResponseBody(@Nullable String responseBody) {
        this.responseBody = responseBody;
        return this;
    }

    public ApiException setResponseCode(@Nullable Integer responseCode) {
        this.responseCode = responseCode;
        return this;
    }

    public ApiException setResponseHeaders(Headers headers) {
        if (headers != null) {
            responseHeaders = new HashMap<>();
            headers.all().forEach(h -> responseHeaders.put(h.getName(), h.getValue()));
        }
        return this;
    }

    @Override
    public String toString() {
        return getMessage() + " 消息体:" + responseBody + " 消息头：" + responseHeaders + " 响应代码：" + responseCode;
    }
}
