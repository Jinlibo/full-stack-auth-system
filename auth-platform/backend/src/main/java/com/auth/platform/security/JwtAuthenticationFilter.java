package com.auth.platform.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
 * JWT 认证过滤器
 *
 * <p>继承 {@link OncePerRequestFilter}，确保每个 HTTP 请求只被此过滤器处理一次
 * （OncePerRequestFilter 内部通过请求属性标记来保证幂等性）。
 *
 * <p>过滤器的执行顺序（在 Spring Security 过滤器链中）：
 * <pre>
 * 客户端请求
 *    → CorsFilter（跨域处理）
 *    → SecurityContextPersistenceFilter（Security 上下文持久化）
 *    → JwtAuthenticationFilter（本过滤器：JWT 解析与认证）  ← 在 UsernamePasswordAuthenticationFilter 之前
 *    → UsernamePasswordAuthenticationFilter（表单登录，本项目不使用）
 *    → ... 其他 Security 过滤器
 *    → FilterSecurityInterceptor（权限校验：@PreAuthorize 注解在此处生效）
 *    → Controller 处理
 * </pre>
 *
 * <p>认证成功后的效果：
 * <ul>
 *   <li>将 {@link UsernamePasswordAuthenticationToken} 写入 {@link SecurityContextHolder}</li>
 *   <li>后续 Spring Security 的权限校验（@PreAuthorize）将基于此认证信息</li>
 *   <li>{@code SecurityContextHolder.getContext().getAuthentication()} 可在任何地方获取当前用户</li>
 * </ul>
 *
 * @author auth-platform
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserDetailsServiceImpl userDetailsService;
    private final StringRedisTemplate redisTemplate;
    private final JwtProperties jwtProperties;

    /**
     * 核心过滤逻辑
     *
     * <p>每次 HTTP 请求都会经过此方法，执行以下步骤：
     * <ol>
     *   <li>从请求头提取 Bearer Token</li>
     *   <li>验证 Token 签名和有效期</li>
     *   <li>检查 Token 是否在 Redis 黑名单中（用户已 logout）</li>
     *   <li>解析 Token 中的用户名，加载 UserDetails（含权限信息）</li>
     *   <li>构建认证对象并写入 SecurityContext</li>
     *   <li>放行请求到下一个过滤器</li>
     * </ol>
     *
     * <p>注意：无论认证是否成功，最终都会调用 {@code filterChain.doFilter()} 放行请求。
     * 若认证失败（Token 无效），SecurityContext 中不会有认证信息，
     * Spring Security 会在后续的权限校验环节拒绝请求（返回 401 或 403）。
     *
     * @param request     当前 HTTP 请求对象
     * @param response    当前 HTTP 响应对象
     * @param filterChain 过滤器链，调用 doFilter 将请求传递给下一个过滤器
     * @throws ServletException Servlet 处理异常
     * @throws IOException      IO 异常（如响应流写入异常）
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        // 第一步：从请求头 Authorization 中提取 Bearer Token
        // 若请求头不存在或格式不正确，extractToken() 返回 null，跳过认证逻辑
        String token = extractToken(request);

        // 第二步：Token 存在且 JWT 签名有效时，进行 Redis 会话验证
        if (StringUtils.hasText(token) && jwtUtil.validateToken(token)) {
            try {
                // 第三步：从 JWT 解出 userId，查 Redis 中存储的 token 值
                Long userId = jwtUtil.getUserIdFromToken(token);
                String storedToken = redisTemplate.opsForValue().get("token:access:" + userId);

                // 第四步：Redis 中的 token 必须与当前请求的 token 完全匹配
                // 不匹配说明：用户已退出、或已在其他设备重新登录（旧 token 被覆盖）
                if (token.equals(storedToken)) {
                    // 第五步：滑动窗口 —— 刷新 Redis TTL（活跃用户永不掉线）
                    redisTemplate.expire("token:access:" + userId,
                            jwtProperties.getExpiration(), TimeUnit.MILLISECONDS);

                    // 第六步：加载用户权限，写入 SecurityContext
                    String username = jwtUtil.parseToken(token).get("username", String.class);
                    UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            } catch (Exception e) {
                log.warn("JWT 认证失败，请求: {} {}，原因: {}",
                        request.getMethod(), request.getRequestURI(), e.getMessage());
            }
        }

        // 第八步：无论认证结果如何，继续执行过滤器链
        // 未认证的请求会在后续的 FilterSecurityInterceptor 中被拦截
        filterChain.doFilter(request, response);
    }

    /**
     * 从 HTTP 请求头中提取 JWT Token
     *
     * <p>标准的 Bearer Token 认证格式：
     * <pre>Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIi4...</pre>
     *
     * <p>提取规则：
     * <ul>
     *   <li>请求头名称必须为 "Authorization"（大小写不敏感，Servlet 规范保证）</li>
     *   <li>值必须以 "Bearer " 开头（注意空格，共 7 个字符）</li>
     *   <li>取 "Bearer " 后面的部分作为 Token</li>
     *   <li>若不满足上述条件，返回 null（不影响请求继续处理）</li>
     * </ul>
     *
     * @param request 当前 HTTP 请求
     * @return 提取出的 JWT 字符串，若不存在或格式不正确则返回 null
     */
    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        // StringUtils.hasText() 检查字符串非空且非纯空白
        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            // 截取 "Bearer "（7 个字符）之后的部分，即实际的 JWT 字符串
            return header.substring(7);
        }
        return null;
    }
}
