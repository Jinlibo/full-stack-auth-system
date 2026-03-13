package com.auth.platform.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;
import java.util.List;

/**
 * 跨域资源共享（CORS）配置
 *
 * <p>CORS（Cross-Origin Resource Sharing）是浏览器的安全机制：
 * 当前端（如 http://localhost:5173）向不同源的后端（http://localhost:8080）发送请求时，
 * 浏览器会先发送一个 OPTIONS 预检请求（Preflight Request）询问服务器是否允许跨域。
 * 服务器通过响应头（Access-Control-Allow-Origin 等）告知浏览器是否放行。
 *
 * <p>Spring Security 与 CORS 的配合：
 * CorsFilter 必须在 Spring Security 过滤器链之前执行，否则 OPTIONS 预检请求
 * 会被 Security 的认证拦截器拦截并返回 401，导致跨域请求失败。
 * 使用 {@link CorsFilter}（Spring Web 的实现，非 Security 内置）确保执行顺序正确。
 *
 * <p>配置来源：
 * 允许的跨域来源通过 application.yml 中的 {@code cors.allowed-origins} 配置，
 * 支持多个来源（逗号分隔），如 {@code "http://localhost:5173,https://your-domain.com"}
 *
 * @author auth-platform
 */
@Configuration
public class CorsConfig {

    /**
     * 从 application.yml 注入允许的跨域来源列表
     * 格式：多个来源用逗号分隔，如 "http://localhost:5173,http://localhost:3000"
     * 生产环境应配置为实际的前端域名，而非使用通配符 "*"
     */
    @Value("${cors.allowed-origins}")
    private String allowedOrigins;

    /**
     * 注册 CORS 过滤器 Bean
     *
     * <p>配置说明：
     * <ul>
     *   <li>allowedOrigins：允许跨域的来源列表（精确匹配，不能用通配符，因为 allowCredentials=true）</li>
     *   <li>allowedMethods：允许的 HTTP 方法，OPTIONS 是预检请求必须，GET/POST/PUT/DELETE 是业务方法</li>
     *   <li>addAllowedHeader("*")：允许所有请求头（包括 Authorization、Content-Type 等）</li>
     *   <li>setAllowCredentials(true)：允许携带凭据（Cookie、Authorization 头等），
     *       这是 JWT 认证必须的，但要求 allowedOrigins 不能为 "*"</li>
     *   <li>setMaxAge(3600)：预检请求（OPTIONS）的缓存时间（秒），
     *       缓存期内同一跨域请求无需重复发送预检，减少网络开销</li>
     * </ul>
     *
     * <p>安全性考量：
     * <ul>
     *   <li>不使用通配符 "*" 的 allowedOrigins，严格限制允许的来源，防止 CSRF 攻击</li>
     *   <li>不暴露敏感响应头（未调用 addExposedHeader），遵循最小权限原则</li>
     *   <li>生产环境应确保 allowed-origins 仅包含受信任的前端域名</li>
     * </ul>
     *
     * @return 配置好的 {@link CorsFilter} Bean，Spring Boot 会自动将其注册到 Servlet 容器
     */
    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();

        // 添加允许的来源：将逗号分隔的配置字符串拆分为数组，逐个添加
        // 使用精确来源而非 "*"，配合 allowCredentials=true 才能正常工作
        Arrays.stream(allowedOrigins.split(","))
                .map(String::trim) // 去除每个来源前后的空白字符
                .forEach(config::addAllowedOrigin);

        // 允许的 HTTP 方法（不包含 PATCH，当前项目未使用）
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));

        // 允许所有请求头（前端会发送 Authorization、Content-Type 等）
        config.addAllowedHeader("*");

        // 允许携带凭据（JWT Token 通过 Authorization 请求头携带，此项必须为 true）
        // 注意：当此项为 true 时，setAllowedOrigins 不能包含 "*"
        config.setAllowCredentials(true);

        // 预检请求（OPTIONS）的结果缓存时间：1 小时（3600 秒）
        // 在此时间内，浏览器会直接发送实际请求，不再重复发送预检请求
        config.setMaxAge(3600L);

        // 将 CORS 配置应用到所有 URL 路径（"/**" 表示匹配所有路径）
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return new CorsFilter(source);
    }
}
