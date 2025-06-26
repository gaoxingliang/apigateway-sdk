package cn.sichuancredit.apigateway.client;

public interface RetryHttpHandler {
    boolean retry(int statusCode, String body);
}
