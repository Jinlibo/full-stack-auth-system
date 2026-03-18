package com.auth.platform.security;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * JWT 配置属性类
 * <p>通过 {@code @ConfigurationProperties} 绑定 application.yml 中 auth.jwt 前缀的配置项，
 * 集中管理 JWT 令牌相关的配置参数。</p>
 */
@Data
@Component
@ConfigurationProperties(prefix = "auth.jwt")
public class JwtProperties {
    /** JWT 签名密钥（Base64 编码），生产环境应通过环境变量注入，至少 256 位（32 字节） */
    private String secret;
    /** Redis session TTL（毫秒），默认 7200000ms（2小时），同时作为 JWT 兜底过期的参考值 */
    private Long expiration = 7200000L;
    /** JWT 签发方标识，用于多服务场景区分 Token 来源，默认为 "auth-platform" */
    private String issuer = "auth-platform";
}
