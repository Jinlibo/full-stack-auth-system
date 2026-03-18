package com.product.app.controller;

import com.product.app.common.R;
import com.product.app.dto.*;
import com.product.app.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 认证控制器，提供账号密码登录、用户注册、OAuth2 登录回调及退出登录接口。
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * 账号密码登录，验证通过后返回 JWT 令牌及用户信息。
     *
     * @param request 登录请求，包含用户名和密码
     * @return 登录响应，包含 accessToken 和用户信息
     */
    @PostMapping("/login")
    public R<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return R.ok(authService.login(request));
    }

    /**
     * 用户注册，创建新账号并分配默认角色。
     *
     * @param request 注册请求，包含用户名、密码、邮箱等信息
     * @return 空数据成功响应
     */
    @PostMapping("/register")
    public R<Void> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        return R.ok();
    }

    /**
     * 获取 OAuth2 授权地址，供前端跳转至授权服务器发起授权流程。
     *
     * @param state 前端传入的随机 state 参数，用于防 CSRF
     * @return 包含 authorizeUrl 的响应
     */
    @GetMapping("/oauth2/authorize-url")
    public R<Map<String, String>> getOAuthUrl(@RequestParam(required = false) String state) {
        String url = authService.getOAuthAuthorizeUrl(state);
        return R.ok(Map.of("authorizeUrl", url));
    }

    /**
     * OAuth2 回调处理，使用授权码换取本地 JWT。
     * 若为首次登录则返回 pendingBind=true 及临时令牌。
     *
     * @param request OAuth2 回调请求，包含授权码和 state
     * @return 登录响应或待绑定响应
     */
    @PostMapping("/oauth2/callback")
    public R<LoginResponse> oauthCallback(@Valid @RequestBody OAuth2CallbackRequest request) {
        return R.ok(authService.oauthLogin(request));
    }

    /**
     * OAuth2 首次登录后选择创建新账号，根据临时令牌自动创建本地账号并绑定 OAuth 信息。
     *
     * @param body 请求体，包含 oauthPendingToken 临时令牌
     * @return 登录响应，包含新账号的 JWT 和用户信息
     */
    @PostMapping("/oauth2/create-new")
    public R<LoginResponse> oauthCreateNew(@RequestBody Map<String, String> body) {
        return R.ok(authService.oauthCreateNew(body.get("oauthPendingToken")));
    }

    /**
     * OAuth2 首次登录后选择绑定已有账号，验证现有账号凭证后完成 OAuth 绑定。
     *
     * @param body 请求体，包含 oauthPendingToken、username 和 password
     * @return 登录响应，包含已有账号的 JWT 和用户信息
     */
    @PostMapping("/oauth2/bind-existing")
    public R<LoginResponse> oauthBindExisting(@RequestBody Map<String, String> body) {
        return R.ok(authService.oauthBindExisting(
                body.get("oauthPendingToken"),
                body.get("username"),
                body.get("password")));
    }

    /**
     * 退出登录，删除 Redis 中存储的 JWT 令牌使其立即失效。
     *
     * @param token 请求头中的 Authorization Bearer 令牌
     * @return 空数据成功响应
     */
    @PostMapping("/logout")
    public R<Void> logout(@RequestHeader("Authorization") String token) {
        authService.logout(token);
        return R.ok();
    }
}
