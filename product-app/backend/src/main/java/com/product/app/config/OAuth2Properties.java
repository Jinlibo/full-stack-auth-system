package com.product.app.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * OAuth2 配置属性类，绑定配置文件中 oauth2 前缀的属性，
 * 包含授权服务器地址信息和客户端注册信息。
 */
@Data
@Component
@ConfigurationProperties(prefix = "oauth2")
public class OAuth2Properties {

    /** 授权服务器相关配置 */
    private AuthServer authServer = new AuthServer();

    /** OAuth2 客户端注册信息 */
    private Client client = new Client();

    /**
     * 授权服务器配置，包含各端点的 URI 地址。
     */
    @Data
    public static class AuthServer {
        /** 授权服务器根地址 */
        private String baseUrl;
        /** 授权端点 URI */
        private String authorizeUri;
        /** 令牌端点 URI */
        private String tokenUri;
        /** 用户信息端点 URI */
        private String userinfoUri;
    }

    /**
     * OAuth2 客户端注册信息，包含客户端凭证和回调配置。
     */
    @Data
    public static class Client {
        /** 客户端 ID */
        private String clientId;
        /** 客户端密钥 */
        private String clientSecret;
        /** OAuth2 授权成功后的回调地址 */
        private String redirectUri;
        /** 申请的权限范围，多个以逗号分隔 */
        private String scope;
    }
}
