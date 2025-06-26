package cn.sichuancredit.apigateway.client;

import java.util.regex.*;

public class DefaultRetryHandler implements RetryHttpHandler {
    /**
     * apisix internal error log eg:
     * request to wolf-server failed
     */
    public static final String REGEX = "(.*)request(.*)failed(.*)";

    @Override
    public boolean retry(int statusCode, String body) {
        return body != null && Pattern.compile(REGEX).matcher(body.trim()).matches();
    }
}
