package com.product.app.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "oauth2")
public class OAuth2Properties {
    private AuthServer authServer = new AuthServer();
    private Client client = new Client();

    @Data
    public static class AuthServer {
        private String baseUrl;
        private String authorizeUri;
        private String tokenUri;
        private String userinfoUri;
    }

    @Data
    public static class Client {
        private String clientId;
        private String clientSecret;
        private String redirectUri;
        private String scope;
    }
}
