package com.auth.platform.config;

import com.auth.platform.security.ProductStatusAwareClientRepository;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import com.auth.platform.config.jackson.LoginUserMixin;
import com.auth.platform.config.jackson.SysUserMixin;
import com.auth.platform.entity.SysUser;
import com.auth.platform.mapper.SysProductMapper;
import com.auth.platform.security.LoginUser;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.jackson2.SecurityJackson2Modules;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.client.JdbcRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.oauth2.server.authorization.jackson2.OAuth2AuthorizationServerJackson2Module;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher;

import javax.sql.DataSource;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.List;

/**
 * OAuth2 授权服务器配置类
 *
 * <p>负责配置 Spring Authorization Server 的核心组件，包括：
 * <ul>
 *   <li>注册客户端仓库（RegisteredClientRepository）：从数据库加载 OAuth2 客户端，并结合产品状态过滤</li>
 *   <li>授权信息服务（OAuth2AuthorizationService）：基于 JDBC 持久化授权码、访问令牌等</li>
 *   <li>授权同意服务（OAuth2AuthorizationConsentService）：持久化用户的授权确认记录</li>
 *   <li>授权服务器安全过滤链（Order=1）：处理 OAuth2/OIDC 相关端点</li>
 *   <li>JWK 密钥源（JWKSource）：从配置文件加载 RSA 密钥对，用于签发和验证 JWT</li>
 *   <li>JWT 解码器（JwtDecoder）：供资源服务器验证 OAuth2 颁发的访问令牌</li>
 *   <li>授权服务器设置（AuthorizationServerSettings）：配置 issuer 等全局参数</li>
 * </ul>
 *
 * @author auth-platform
 */
@Configuration
public class AuthorizationServerConfig {

    /** 前端应用地址，用于配置 OAuth2 登录页和授权确认页的跳转 URL */
    @Value("${app.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    /** 数据源，用于创建 JDBC 模板以操作 OAuth2 相关数据库表 */
    @Autowired
    private DataSource dataSource;

    /** 产品 Mapper，用于 ProductStatusAwareClientRepository 校验产品启用状态 */
    @Autowired
    private SysProductMapper productMapper;

    /** JWK 配置属性，从 application.yml 中读取 RSA 公私钥和 Key ID */
    @Autowired
    private JwkProperties jwkProperties;

    /**
     * 注册 OAuth2 客户端仓库 Bean
     *
     * <p>使用 {@code ProductStatusAwareClientRepository} 包装标准的
     * {@code JdbcRegisteredClientRepository}，在查询客户端时额外检查对应产品的状态，
     * 若产品已禁用则返回 null（相当于客户端不存在），阻止 OAuth2 授权流程继续。
     *
     * @return 带产品状态感知的已注册客户端仓库
     */
    @Bean
    public RegisteredClientRepository registeredClientRepository() {
        JdbcRegisteredClientRepository jdbc = new JdbcRegisteredClientRepository(new JdbcTemplate(dataSource));
        return new ProductStatusAwareClientRepository(jdbc, productMapper);
    }

    /**
     * 注册 OAuth2 授权信息服务 Bean
     *
     * <p>基于 JDBC 将授权码、访问令牌、刷新令牌等信息持久化到数据库。
     * 配置了自定义的 ObjectMapper 以正确序列化/反序列化包含 LoginUser 和 SysUser 的认证对象。
     *
     * @param registeredClientRepository 已注册客户端仓库
     * @return 配置好 Jackson 序列化的 JDBC OAuth2 授权服务
     */
    @Bean
    public OAuth2AuthorizationService authorizationService(RegisteredClientRepository registeredClientRepository) {
        JdbcOAuth2AuthorizationService service =
                new JdbcOAuth2AuthorizationService(new JdbcTemplate(dataSource), registeredClientRepository);

        ObjectMapper objectMapper = buildAuthorizationObjectMapper();

        // 配置读取路径：反序列化数据库中存储的 Authentication 对象（包含 LoginUser 主体）
        JdbcOAuth2AuthorizationService.OAuth2AuthorizationRowMapper rowMapper =
                new JdbcOAuth2AuthorizationService.OAuth2AuthorizationRowMapper(registeredClientRepository);
        rowMapper.setObjectMapper(objectMapper);
        service.setAuthorizationRowMapper(rowMapper);

        // 配置写入路径：序列化 Authentication 对象时保证 @class 类型信息一致性
        JdbcOAuth2AuthorizationService.OAuth2AuthorizationParametersMapper parametersMapper =
                new JdbcOAuth2AuthorizationService.OAuth2AuthorizationParametersMapper();
        parametersMapper.setObjectMapper(objectMapper);
        service.setAuthorizationParametersMapper(parametersMapper);

        return service;
    }

    /**
     * 构建用于 OAuth2 授权信息序列化的 ObjectMapper
     *
     * <p>注册了以下模块：
     * <ul>
     *   <li>Spring Security Jackson2 模块：支持 Security 内置类型的序列化</li>
     *   <li>OAuth2AuthorizationServerJackson2Module：支持 OAuth2 授权服务器相关类型</li>
     *   <li>JavaTimeModule：支持 SysUser 中的 LocalDateTime 字段</li>
     *   <li>LoginUserMixin / SysUserMixin：允许自定义认证主体被正确反序列化</li>
     * </ul>
     *
     * @return 配置好的 ObjectMapper 实例
     */
    private ObjectMapper buildAuthorizationObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        ClassLoader classLoader = JdbcOAuth2AuthorizationService.class.getClassLoader();
        List<Module> securityModules = SecurityJackson2Modules.getModules(classLoader);
        objectMapper.registerModules(securityModules);
        objectMapper.registerModule(new OAuth2AuthorizationServerJackson2Module());
        // 注册 JavaTimeModule：支持 SysUser 中的 LocalDateTime 字段序列化
        objectMapper.registerModule(new JavaTimeModule());
        // 注册 Mixin：允许 LoginUser 和 SysUser 被正确反序列化
        objectMapper.addMixIn(LoginUser.class, LoginUserMixin.class);
        objectMapper.addMixIn(SysUser.class, SysUserMixin.class);
        return objectMapper;
    }

    /**
     * 注册 OAuth2 授权同意服务 Bean
     *
     * <p>将用户对各客户端的授权确认（Consent）持久化到数据库，
     * 使用户在已同意的情况下再次授权时无需重复确认。
     *
     * @param registeredClientRepository 已注册客户端仓库
     * @return 基于 JDBC 的授权同意服务
     */
    @Bean
    public OAuth2AuthorizationConsentService authorizationConsentService(RegisteredClientRepository registeredClientRepository) {
        return new JdbcOAuth2AuthorizationConsentService(new JdbcTemplate(dataSource), registeredClientRepository);
    }

    /**
     * 配置授权服务器安全过滤链（优先级 Order=1，最高优先级）
     *
     * <p>处理所有 OAuth2/OIDC 标准端点（/oauth2/authorize、/oauth2/token 等）。
     * 主要配置：
     * <ul>
     *   <li>开启 OIDC 支持</li>
     *   <li>自定义授权确认页地址指向前端 Vue SPA</li>
     *   <li>未认证的 HTML 请求重定向到前端登录页</li>
     *   <li>启用 JWT 资源服务器支持（用于内省 token）</li>
     *   <li>关闭 CSRF（前后端分离项目不需要）</li>
     * </ul>
     *
     * @param http HttpSecurity 构建器
     * @return 构建好的授权服务器安全过滤链
     * @throws Exception 配置过程中的异常
     */
    @Bean
    @Order(1)
    public SecurityFilterChain authorizationServerSecurityFilterChain(HttpSecurity http) throws Exception {
        OAuth2AuthorizationServerConfiguration.applyDefaultSecurity(http);
        http.getConfigurer(OAuth2AuthorizationServerConfigurer.class)
                .oidc(Customizer.withDefaults())
                .authorizationEndpoint(e -> e.consentPage(frontendUrl + "/oauth-consent"));
        http
                .exceptionHandling(exceptions -> exceptions
                        .defaultAuthenticationEntryPointFor(
                                new LoginUrlAuthenticationEntryPoint(frontendUrl + "/oauth-login"),
                                new MediaTypeRequestMatcher(MediaType.TEXT_HTML)
                        )
                )
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
                .csrf(AbstractHttpConfigurer::disable);
        return http.build();
    }

    /**
     * 注册 JWK 密钥源 Bean
     *
     * <p>从 {@link JwkProperties} 中读取 Base64 编码的 RSA 公私钥，构建 JWK Set，
     * 用于 JWT 的签名（私钥）和验证（公钥）。
     *
     * @return 包含 RSA 密钥对的不可变 JWK Set
     * @throws IllegalStateException 若密钥加载失败（配置错误）
     */
    @Bean
    public JWKSource<SecurityContext> jwkSource() {
        try {
            RSAPublicKey publicKey = (RSAPublicKey) KeyFactory.getInstance("RSA")
                    .generatePublic(new X509EncodedKeySpec(decodePem(jwkProperties.getPublicKey())));
            RSAPrivateKey privateKey = (RSAPrivateKey) KeyFactory.getInstance("RSA")
                    .generatePrivate(new PKCS8EncodedKeySpec(decodePem(jwkProperties.getPrivateKey())));
            RSAKey rsaKey = new RSAKey.Builder(publicKey)
                    .privateKey(privateKey)
                    .keyID(jwkProperties.getKeyId())
                    .build();
            return new ImmutableJWKSet<>(new JWKSet(rsaKey));
        } catch (Exception e) {
            throw new IllegalStateException("无法加载 JWK RSA 密钥，请检查 app.jwk 配置", e);
        }
    }

    /**
     * 解析 PEM 格式的 Base64 编码密钥字节
     *
     * <p>移除 PEM 头尾行（-----BEGIN...-----）和所有空白字符后，进行 Base64 解码。
     *
     * @param pem PEM 格式或纯 Base64 编码的密钥字符串
     * @return 解码后的原始密钥字节数组
     */
    private static byte[] decodePem(String pem) {
        return Base64.getDecoder().decode(pem.replaceAll("-----.*-----", "").replaceAll("\\s", ""));
    }

    /**
     * 注册 JWT 解码器 Bean
     *
     * <p>基于 JWK 密钥源构建解码器，用于验证 OAuth2 授权服务器颁发的访问令牌签名。
     * 在 {@link com.auth.platform.controller.OAuth2UserInfoController} 中手动注入使用。
     *
     * @param jwkSource JWK 密钥源
     * @return JWT 解码器
     */
    @Bean
    public JwtDecoder jwtDecoder(JWKSource<SecurityContext> jwkSource) {
        return OAuth2AuthorizationServerConfiguration.jwtDecoder(jwkSource);
    }

    /**
     * 注册授权服务器全局设置 Bean
     *
     * <p>配置授权服务器的发行者（issuer）地址，该地址会写入颁发的 JWT 的 iss 字段，
     * 客户端可以通过此地址发现授权服务器的 OIDC 配置端点（/.well-known/openid-configuration）。
     *
     * @return 授权服务器设置对象
     */
    @Bean
    public AuthorizationServerSettings authorizationServerSettings() {
        return AuthorizationServerSettings.builder()
                .issuer("http://localhost:8080")
                .build();
    }
}
