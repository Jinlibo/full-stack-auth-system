package com.auth.platform.security;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "auth.jwt")
public class JwtProperties {
    private String secret;
    private Long expiration = 7200000L;  // Redis session TTL（2小时），同时作为 JWT 兜底过期的参考值
    private String issuer = "auth-platform";
}
