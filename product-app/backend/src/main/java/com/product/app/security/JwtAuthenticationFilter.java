package com.product.app.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * JWT 认证过滤器，每次请求只执行一次，从请求头中提取并验证 JWT，
 * 并结合 Redis 校验令牌有效性，通过后将用户信息写入 Spring Security 上下文。
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserDetailsServiceImpl userDetailsService;
    private final StringRedisTemplate redisTemplate;

    /**
     * 过滤器核心逻辑：解析 Authorization 请求头中的 Bearer Token，
     * 验证 JWT 有效性并与 Redis 中存储的令牌比对，通过后刷新令牌 TTL（滑动窗口），
     * 并将认证信息写入 SecurityContextHolder。
     *
     * @param request  HTTP 请求
     * @param response HTTP 响应
     * @param chain    过滤器链
     * @throws ServletException Servlet 异常
     * @throws IOException      IO 异常
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            if (jwtUtil.validateToken(token)) {
                try {
                    Long userId = jwtUtil.getUserIdFromToken(token);
                    String storedToken = redisTemplate.opsForValue().get("token:access:" + userId);
                    if (token.equals(storedToken)) {
                        // 滑动窗口：刷新 Redis TTL，保持活跃用户的会话持续有效
                        redisTemplate.expire("token:access:" + userId,
                                jwtUtil.getExpiration(), TimeUnit.MILLISECONDS);
                        String username = jwtUtil.parseToken(token).get("username", String.class);
                        UserDetails ud = userDetailsService.loadUserByUsername(username);
                        var auth = new UsernamePasswordAuthenticationToken(ud, null, ud.getAuthorities());
                        auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(auth);
                    }
                } catch (Exception ignored) {
                }
            }
        }
        chain.doFilter(request, response);
    }
}
