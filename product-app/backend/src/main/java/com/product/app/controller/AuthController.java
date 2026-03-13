package com.product.app.controller;

import com.product.app.common.R;
import com.product.app.dto.*;
import com.product.app.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * 账号密码登录
     */
    @PostMapping("/login")
    public R<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return R.ok(authService.login(request));
    }

    /**
     * 获取OAuth2授权地址(前端重定向用)
     */
    @GetMapping("/oauth2/authorize-url")
    public R<Map<String, String>> getOAuthUrl(@RequestParam(required = false) String state) {
        String url = authService.getOAuthAuthorizeUrl(state);
        return R.ok(Map.of("authorizeUrl", url));
    }

    /**
     * OAuth2回调 - 用授权码换取本地JWT
     */
    @PostMapping("/oauth2/callback")
    public R<LoginResponse> oauthCallback(@Valid @RequestBody OAuth2CallbackRequest request) {
        return R.ok(authService.oauthLogin(request));
    }

    @PostMapping("/logout")
    public R<Void> logout(@RequestHeader("Authorization") String token) {
        authService.logout(token);
        return R.ok();
    }
}
