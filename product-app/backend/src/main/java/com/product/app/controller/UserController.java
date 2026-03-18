package com.product.app.controller;

import com.product.app.common.PageQuery;
import com.product.app.common.R;
import com.product.app.dto.OAuth2CallbackRequest;
import com.product.app.dto.PasswordRequest;
import com.product.app.dto.UserInfo;
import com.product.app.dto.UserUpdateRequest;
import com.product.app.security.LoginUser;
import com.product.app.service.AppUserService;
import com.product.app.service.AuthService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 用户管理控制器，提供当前用户信息查询、资料修改、密码设置及 OAuth 绑定/解绑等接口。
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final AppUserService userService;
    private final AuthService authService;

    /**
     * 获取当前登录用户的详细信息，包含角色、权限及 OAuth 绑定情况。
     *
     * @param loginUser 当前登录用户，由 Spring Security 注入
     * @return 当前用户信息
     */
    @GetMapping("/me")
    public R<UserInfo> getCurrentUser(@AuthenticationPrincipal LoginUser loginUser) {
        return R.ok(userService.getUserInfo(loginUser.getUser().getId()));
    }

    /**
     * 修改当前登录用户的个人资料（邮箱、手机号、昵称、头像）。
     *
     * @param loginUser 当前登录用户
     * @param request   用户资料更新请求
     * @return 空数据成功响应
     */
    @PutMapping("/me/profile")
    public R<Void> updateProfile(@AuthenticationPrincipal LoginUser loginUser,
                                 @RequestBody UserUpdateRequest request) {
        userService.updateProfile(loginUser.getUser().getId(), request);
        return R.ok();
    }

    /**
     * 分页查询用户列表，支持按用户名或昵称关键词搜索。
     * 需要 user:list 权限。
     *
     * @param query 分页查询参数
     * @return 用户分页数据
     */
    @GetMapping
    @PreAuthorize("hasAuthority('user:list')")
    public R<IPage<UserInfo>> pageUsers(PageQuery query) {
        return R.ok(userService.pageUsers(query));
    }

    /**
     * 根据用户 ID 查询指定用户的详细信息。
     * 需要 user:list 权限。
     *
     * @param id 用户 ID
     * @return 用户信息
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('user:list')")
    public R<UserInfo> getUser(@PathVariable Long id) {
        return R.ok(userService.getUserInfo(id));
    }

    /**
     * 设置或修改当前登录用户的密码。
     * 首次设置（无密码用户）无需旧密码；已有密码则需要先验证旧密码。
     *
     * @param loginUser 当前登录用户
     * @param request   密码设置请求，包含旧密码（可选）和新密码
     * @return 空数据成功响应
     */
    @PutMapping("/me/password")
    public R<Void> setPassword(@AuthenticationPrincipal LoginUser loginUser,
                               @Valid @RequestBody PasswordRequest request) {
        userService.setPassword(loginUser.getUser().getId(), request);
        return R.ok();
    }

    /**
     * 将 OAuth2 账号绑定到当前已登录用户。
     *
     * @param loginUser 当前登录用户
     * @param request   OAuth2 回调请求，包含授权码和 state
     * @return 绑定后的最新用户信息
     */
    @PostMapping("/me/oauth/bind")
    public R<UserInfo> bindOAuth(@AuthenticationPrincipal LoginUser loginUser,
                                 @Valid @RequestBody OAuth2CallbackRequest request) {
        return R.ok(authService.bindOAuth(loginUser.getUser().getId(), request));
    }

    /**
     * 解绑当前登录用户指定 provider 的 OAuth2 账号，同时撤销授权服务器的授权同意。
     *
     * @param loginUser 当前登录用户
     * @param provider  OAuth2 提供商标识，如 auth-platform
     * @return 空数据成功响应
     */
    @DeleteMapping("/me/oauth/{provider}")
    public R<Void> unbindOAuth(@AuthenticationPrincipal LoginUser loginUser,
                               @PathVariable String provider) {
        userService.unbindOAuth(loginUser.getUser().getId(), provider);
        return R.ok();
    }
}
