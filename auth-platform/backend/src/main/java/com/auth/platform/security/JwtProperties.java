package com.auth.platform.security;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "auth.jwt")
public class JwtProperties {
    private String secret;
    private Long expiration = 7200000L;
    private Long refreshExpiration = 604800000L;
    private String issuer = "auth-platform";
}
