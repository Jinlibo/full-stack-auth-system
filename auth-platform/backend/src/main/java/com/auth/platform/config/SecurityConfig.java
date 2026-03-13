package com.auth.platform.config;

import com.auth.platform.security.JwtAuthenticationFilter;
import com.auth.platform.security.LoginUser;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
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
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Value("${app.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * Order 2: session-based form login for the OAuth2 authorization code flow.
     * Processes credentials POSTed from the custom Vue SPA login page (oauth-login).
     * On success: returns JSON {"redirectUrl":"..."} so the SPA can navigate.
     * On failure: returns JSON {"message":"..."} with 401 status.
     * A server-side session is created so the auth server (Order 1) finds the
     * principal when the browser navigates back to /oauth2/authorize.
     */
    @Bean
    @Order(2)
    public SecurityFilterChain formLoginFilterChain(HttpSecurity http) throws Exception {
        HttpSessionRequestCache requestCache = new HttpSessionRequestCache();
        http
                .securityMatcher("/login", "/logout", "/error")
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .formLogin(form -> form
                        .successHandler((request, response, authentication) -> {
                            response.setContentType("application/json;charset=UTF-8");
                            SavedRequest savedRequest = requestCache.getRequest(request, response);
                            String redirectUrl = savedRequest != null
                                    ? savedRequest.getRedirectUrl()
                                    : frontendUrl;
                            // 将当前登录用户的昵称/用户名一并返回，供 OAuthConsent 页面展示
                            // 这样授权确认页可以显示 "你好，xxx，以下应用请求授权"
                            String nickname = "";
                            String username = authentication.getName();
                            if (authentication.getPrincipal() instanceof LoginUser loginUser) {
                                String n = loginUser.getUser().getNickname();
                                nickname = (n != null && !n.isEmpty()) ? n : loginUser.getUsername();
                            }
                            // 对昵称中的双引号转义，防止 JSON 注入
                            nickname = nickname.replace("\\", "\\\\").replace("\"", "\\\"");
                            username = username.replace("\\", "\\\\").replace("\"", "\\\"");
                            response.getWriter().write(
                                    "{\"redirectUrl\":\"" + redirectUrl + "\"" +
                                            ",\"nickname\":\"" + nickname + "\"" +
                                            ",\"username\":\"" + username + "\"}"
                            );
                        })
                        .failureHandler((request, response, exception) -> {
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType("application/json;charset=UTF-8");
                            String msg = exception.getMessage() != null
                                    ? exception.getMessage().replace("\"", "'")
                                    : "用户名或密码错误";
                            response.getWriter().write("{\"message\":\"" + msg + "\"}");
                        })
                        .permitAll()
                )
                .csrf(AbstractHttpConfigurer::disable);
        return http.build();
    }

    /**
     * Order 3: stateless JWT filter chain for all REST API calls.
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
                                "/api/oauth2/**"
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
