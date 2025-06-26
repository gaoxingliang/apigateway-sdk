package cn.sichuancredit.apigateway.client;

import lombok.*;

@Setter
@Getter
@NoArgsConstructor
public class ApiConfig {
    String username;
    String password;
    String publicKey;
    String privateKey;
    String url;
    int tokenExpireTimeInSeconds = 60 * 30;
    int readTimeoutInSeconds = 45;
    int connectTimeoutInSeconds = 10;
    String proxyHost;
    int proxyPort;
    String proxyUserName;
    String proxyPassword;
    /**
     * Automaticly retry certain recoverable errors like socket timeouts.
     */
    boolean automaticRetries = false;

    String app = "data";

    RetryHttpHandler retryHttpHandler = new DefaultRetryHandler();
}
