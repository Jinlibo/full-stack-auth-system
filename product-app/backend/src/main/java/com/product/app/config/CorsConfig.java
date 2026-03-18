package com.product.app.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;
import java.util.List;

/**
 * 跨域资源共享（CORS）配置类，根据配置文件中允许的源地址构建全局跨域过滤器。
 */
@Configuration
public class CorsConfig {

    /** 允许跨域的来源地址，多个地址以英文逗号分隔 */
    @Value("${cors.allowed-origins}")
    private String allowedOrigins;

    /**
     * 创建并注册全局跨域过滤器 Bean，允许指定源、常用 HTTP 方法及全部请求头，并支持携带凭证。
     *
     * @return 配置完毕的 CorsFilter 实例
     */
    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        Arrays.stream(allowedOrigins.split(",")).forEach(config::addAllowedOrigin);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.addAllowedHeader("*");
        config.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}
