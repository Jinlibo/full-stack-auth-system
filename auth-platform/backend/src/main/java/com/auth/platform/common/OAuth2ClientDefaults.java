package com.auth.platform.common;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;

import java.time.Duration;

/**
 * Default OAuth2 client configuration constants shared between
 * DataInitializer and SysProductServiceImpl.
 * <p>
 * CLIENT_SETTINGS and TOKEN_SETTINGS are generated via Spring API at class-load time,
 * producing the exact JSON format that Spring Authorization Server expects.
 */
public final class OAuth2ClientDefaults {

    public static final String CLIENT_AUTH_METHODS = "client_secret_basic,client_secret_post";
    public static final String GRANT_TYPES = "authorization_code,refresh_token";
    public static final String SCOPES = "openid,profile,email";

    public static final String CLIENT_SETTINGS;
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

    private OAuth2ClientDefaults() {
    }
}
