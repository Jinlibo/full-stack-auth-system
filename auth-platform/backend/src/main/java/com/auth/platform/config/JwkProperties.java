package com.auth.platform.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * JWK（JSON Web Key）RSA 密钥配置属性类
 *
 * <p>从 application.yml 中的 {@code app.jwk} 前缀读取 RSA 密钥配置，
 * 用于 Spring Authorization Server 签发和验证 JWT 访问令牌。
 *
 * <p>对应 application.yml 配置示例：
 * <pre>
 * app:
 *   jwk:
 *     private-key: "MIIEvAIBADANBgkqhkiG9..."
 *     public-key: "MIIBIjANBgkqhkiG9..."
 *     key-id: "auth-platform-rsa-key"
 * </pre>
 *
 * @author auth-platform
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.jwk")
public class JwkProperties {

    /** RSA 私钥（PKCS8 格式，Base64 编码），用于对 JWT 进行签名 */
    private String privateKey;

    /** RSA 公钥（X.509 格式，Base64 编码），用于验证 JWT 签名 */
    private String publicKey;

    /** JWK Key ID，用于标识密钥，写入 JWT Header 的 kid 字段，默认值为 auth-platform-rsa-key */
    private String keyId = "auth-platform-rsa-key";
}
