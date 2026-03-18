package com.auth.platform.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app.jwk")
public class JwkProperties {
    private String privateKey;
    private String publicKey;
    private String keyId = "auth-platform-rsa-key";
}
