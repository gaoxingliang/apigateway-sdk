package cn.sichuancredit.apigateway.client;

import lombok.*;

import java.net.*;

@Setter
@Getter
@NoArgsConstructor
public class ApiConfig {
    String username;
    String password;
    String publicKey;
    String privateKey;
    String url;
    int tokenExpireTimeInSeconds;
    int readTimeoutInSeconds = 45;
    int connectTimeoutInSeconds = 10;
    Proxy proxy;
}
