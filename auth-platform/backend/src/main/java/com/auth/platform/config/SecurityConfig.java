package com.auth.platform.config;

import com.auth.platform.security.JwtAuthenticationEntryPoint;
import com.auth.platform.security.JwtAuthenticationFilter;
import com.auth.platform.security.LoginFailureHandler;
import com.auth.platform.security.LoginSuccessHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security 主安全配置类
 *
 * <p>定义了三条安全过滤链，按 {@code @Order} 优先级依次匹配请求：
 * <ol>
 *   <li>Order=1（AuthorizationServerConfig）：处理 OAuth2/OIDC 授权服务器端点</li>
 *   <li>Order=2（本类 formLoginFilterChain）：处理 OAuth2 授权码流程的 Session 登录</li>
 *   <li>Order=3（本类 apiSecurityFilterChain）：处理所有 REST API 请求，无状态 JWT 认证</li>
 * </ol>
 *
 * <p>开启了以下 Spring Security 特性：
 * <ul>
 *   <li>{@code @EnableWebSecurity}：激活 Spring Security Web 安全支持</li>
 *   <li>{@code @EnableMethodSecurity}：激活方法级别的权限控制（{@code @PreAuthorize}）</li>
 * </ul>
 *
 * @author auth-platform
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    /** JWT 认证过滤器，在 UsernamePasswordAuthenticationFilter 之前执行，验证请求头中的 Token */
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    private final LoginSuccessHandler loginSuccessHandler;
    private final LoginFailureHandler loginFailureHandler;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    /**
     * 注册密码编码器 Bean
     *
     * <p>使用 BCrypt 算法，强度因子默认为 10，每次编码结果不同（含随机盐），
     * 安全性高，适合存储用户密码哈希。
     *
     * @return BCrypt 密码编码器
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * 注册认证管理器 Bean
     *
     * <p>从 Spring Security 的 {@link AuthenticationConfiguration} 中获取全局认证管理器，
     * 供 {@code AuthServiceImpl} 的 {@code login} 方法调用
     * {@code authenticationManager.authenticate()} 验证用户名密码。
     *
     * @param config Spring Security 认证配置
     * @return 全局认证管理器
     * @throws Exception 获取认证管理器失败时抛出
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * Order=2：基于 Session 的表单登录过滤链，用于 OAuth2 授权码流程
     *
     * <p>处理来自前端 Vue SPA 自定义登录页（/oauth-login）POST 的凭证。
     * <ul>
     *   <li>登录成功：返回 JSON {@code {"redirectUrl":"...", "nickname":"...", "username":"..."}}，
     *       SPA 根据 redirectUrl 导航回 OAuth2 授权端点</li>
     *   <li>登录失败：返回 HTTP 401 和 JSON {@code {"message":"..."}}</li>
     *   <li>服务端 Session 创建：授权服务器（Order=1）处理 /oauth2/authorize 时
     *       需要从 Session 中读取已认证的 Principal</li>
     * </ul>
     *
     * @param http HttpSecurity 构建器
     * @return 构建好的表单登录过滤链
     * @throws Exception 配置过程中的异常
     */
    @Bean
    @Order(2)
    public SecurityFilterChain formLoginFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/login", "/logout", "/error")
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .formLogin(form -> form
                        .successHandler(loginSuccessHandler)
                        .failureHandler(loginFailureHandler)
                        .permitAll()
                )
                .csrf(AbstractHttpConfigurer::disable);
        return http.build();
    }

    /**
     * Order=3：无状态 JWT 过滤链，用于所有 REST API 请求
     *
     * <p>配置说明：
     * <ul>
     *   <li>关闭 CSRF：前后端分离项目使用 JWT 认证，无需 CSRF 保护</li>
     *   <li>无状态 Session：不创建 HttpSession，每次请求通过 JWT 独立认证</li>
     *   <li>白名单路径：登录、注册、OAuth2、Swagger 等接口无需认证</li>
     *   <li>JWT 过滤器：在 UsernamePasswordAuthenticationFilter 之前执行，
     *       解析 Authorization 头中的 Bearer Token 并设置 SecurityContext</li>
     * </ul>
     *
     * @param http HttpSecurity 构建器
     * @return 构建好的 REST API 安全过滤链
     * @throws Exception 配置过程中的异常
     */
    @Bean
    @Order(3)
    public SecurityFilterChain apiSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/auth/login",
                                "/api/auth/register",
                                "/api/auth/refresh",
                                "/oauth2/**",
                                "/login",
                                "/.well-known/**",
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/doc.html/**",
                                "/webjars/**",
                                "/api/oauth2/client-info",
                                "/api/oauth2/userinfo",
                                "/api/oauth2/revoke-consent"
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                .exceptionHandling(ex -> ex.authenticationEntryPoint(jwtAuthenticationEntryPoint))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
