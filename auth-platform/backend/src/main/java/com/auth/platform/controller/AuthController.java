package com.auth.platform.controller;

import com.auth.platform.common.R;
import com.auth.platform.dto.*;
import com.auth.platform.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 认证控制器（Authentication Controller）
 *
 * <p>负责处理用户认证相关的 HTTP 请求，包括：
 * <ul>
 *   <li>登录（login）：验证用户名密码，返回 JWT Access Token 和 Refresh Token</li>
 *   <li>注册（register）：创建新用户账号，自动分配默认角色</li>
 *   <li>刷新 Token（refresh）：使用 Refresh Token 换取新的 Access Token</li>
 *   <li>退出登录（logout）：将当前 Token 加入黑名单使其失效</li>
 * </ul>
 *
 * <p>安全说明：
 * <ul>
 *   <li>此 Controller 的接口路径均在 SecurityConfig 的白名单中，无需 JWT 认证即可访问</li>
 *   <li>具体认证逻辑委托给 {@link AuthService} 实现，Controller 层仅负责参数接收和响应封装</li>
 * </ul>
 *
 * @author auth-platform
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    /**
     * 认证服务，实际的认证业务逻辑在 AuthServiceImpl 中实现
     * 使用接口而非实现类，符合依赖倒置原则（DIP），方便后续替换或 Mock 测试
     */
    private final AuthService authService;

    /**
     * 用户登录接口
     *
     * <p>请求示例：
     * <pre>POST /api/auth/login
     * Content-Type: application/json
     * {
     *   "username": "admin",
     *   "password": "admin123"
     * }</pre>
     *
     * <p>响应示例：
     * <pre>{
     *   "code": 200,
     *   "message": "success",
     *   "data": {
     *     "accessToken": "eyJhbGci...",
     *     "refreshToken": "eyJhbGci...",
     *     "expiresIn": 7200,
     *     "userInfo": { ... }
     *   }
     * }</pre>
     *
     * <p>失败情况：
     * <ul>
     *   <li>用户名或密码错误 → HTTP 401，由 GlobalExceptionHandler 处理 BadCredentialsException</li>
     *   <li>账号被禁用 → HTTP 401，由 UserDetailsServiceImpl 抛出异常</li>
     *   <li>参数校验失败（如 username 为空）→ HTTP 400，由 @Valid 触发</li>
     * </ul>
     *
     * @param request 登录请求体，包含 username 和 password，@Valid 触发 Jakarta Validation 校验
     * @return 包含 JWT 令牌和用户信息的统一响应体
     */
    @PostMapping("/login")
    public R<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return R.ok(authService.login(request));
    }

    /**
     * 用户注册接口
     *
     * <p>请求示例：
     * <pre>POST /api/auth/register
     * Content-Type: application/json
     * {
     *   "username": "newuser",
     *   "password": "password123",
     *   "email": "user@example.com",
     *   "nickname": "新用户"
     * }</pre>
     *
     * <p>注册成功后不会自动登录，前端需要引导用户到登录页进行登录。
     * 后端会自动为新用户分配"普通用户"角色（id=3）。
     *
     * @param request 注册请求体，@Valid 触发 Jakarta Validation 校验（如 @NotBlank、@Email 等）
     * @return 无 data 的成功响应（R<Void>）
     */
    @PostMapping("/register")
    public R<Void> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        return R.ok();
    }

    /**
     * 退出登录接口
     *
     * <p>将当前 Access Token 加入 Redis 黑名单，使该 Token 立即失效。
     * 即使 Token 未过期，后续携带该 Token 的请求也会在 JwtAuthenticationFilter 中被拦截。
     *
     * <p>请求示例：
     * <pre>POST /api/auth/logout
     * Authorization: Bearer eyJhbGci...</pre>
     *
     * <p>黑名单机制：
     * <ul>
     *   <li>Redis Key：token:blacklist:{token}（存完整 Token 字符串）</li>
     *   <li>TTL：与 Token 本身的剩余有效期一致（确保 Token 过期后自动清理）</li>
     *   <li>同时删除 Redis 中的 token:access:{userId} 缓存</li>
     * </ul>
     *
     * @param token 请求头中的 Authorization 值（格式：Bearer xxx），Spring 自动注入
     * @return 无 data 的成功响应
     */
    @PostMapping("/logout")
    public R<Void> logout(@RequestHeader("Authorization") String token) {
        authService.logout(token);
        return R.ok();
    }
}
