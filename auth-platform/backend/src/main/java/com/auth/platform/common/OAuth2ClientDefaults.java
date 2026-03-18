package com.auth.platform.common;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;

import java.time.Duration;

/**
 * OAuth2 客户端默认配置常量类
 *
 * <p>集中定义 OAuth2 客户端的公共默认配置，供 {@code DataInitializer}
 * 和 {@code SysProductServiceImpl} 复用，避免重复定义。
 *
 * <p>{@code CLIENT_SETTINGS} 和 {@code TOKEN_SETTINGS} 在类加载时通过 Spring API
 * 生成，产生符合 Spring Authorization Server 期望格式的精确 JSON 字符串。
 *
 * @author auth-platform
 */
public final class OAuth2ClientDefaults {

    /** 支持的客户端认证方式：Basic 认证和 POST 参数认证 */
    public static final String CLIENT_AUTH_METHODS = "client_secret_basic,client_secret_post";

    /** 支持的授权类型：授权码模式和刷新令牌 */
    public static final String GRANT_TYPES = "authorization_code,refresh_token";

    /** 默认请求的 OAuth2 范围：OpenID Connect、用户资料和邮箱 */
    public static final String SCOPES = "openid,profile,email";

    /** 序列化为 JSON 的客户端设置（由 Spring ClientSettings API 生成） */
    public static final String CLIENT_SETTINGS;

    /** 序列化为 JSON 的令牌设置（由 Spring TokenSettings API 生成） */
    public static final String TOKEN_SETTINGS;

    static {
        ObjectMapper mapper = new ObjectMapper();
        mapper.activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL
        );
        try {
            CLIENT_SETTINGS = mapper.writeValueAsString(
                    ClientSettings.builder()
                            .requireProofKey(false)
                            .requireAuthorizationConsent(true)
                            .build()
                            .getSettings()
            );
            TOKEN_SETTINGS = mapper.writeValueAsString(
                    TokenSettings.builder()
                            .accessTokenTimeToLive(Duration.ofHours(1))
                            .refreshTokenTimeToLive(Duration.ofDays(1))
                            .authorizationCodeTimeToLive(Duration.ofMinutes(5))
                            .idTokenSignatureAlgorithm(SignatureAlgorithm.RS256)
                            .reuseRefreshTokens(true)
                            .build()
                            .getSettings()
            );
        } catch (JsonProcessingException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    /**
     * 私有构造函数，防止实例化
     * 本类为纯常量工具类，不应被实例化
     */
    private OAuth2ClientDefaults() {
    }
}
