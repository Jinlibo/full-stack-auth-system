package com.product.app.controller;

import com.product.app.common.PageQuery;
import com.product.app.common.R;
import com.product.app.dto.OAuth2CallbackRequest;
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

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final AppUserService userService;
    private final AuthService authService;

    @GetMapping("/me")
    public R<UserInfo> getCurrentUser(@AuthenticationPrincipal LoginUser loginUser) {
        return R.ok(userService.getUserInfo(loginUser.getUser().getId()));
    }

    @PutMapping("/me/profile")
    public R<Void> updateProfile(@AuthenticationPrincipal LoginUser loginUser,
                                 @RequestBody UserUpdateRequest request) {
        userService.updateProfile(loginUser.getUser().getId(), request);
        return R.ok();
    }

    @GetMapping
    @PreAuthorize("hasAuthority('user:list')")
    public R<IPage<UserInfo>> pageUsers(PageQuery query) {
        return R.ok(userService.pageUsers(query));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('user:list')")
    public R<UserInfo> getUser(@PathVariable Long id) {
        return R.ok(userService.getUserInfo(id));
    }

    /**
     * 绑定OAuth账号到当前已登录用户
     */
    @PostMapping("/me/oauth/bind")
    public R<UserInfo> bindOAuth(@AuthenticationPrincipal LoginUser loginUser,
                                 @Valid @RequestBody OAuth2CallbackRequest request) {
        return R.ok(authService.bindOAuth(loginUser.getUser().getId(), request));
    }

    /**
     * 解绑指定provider的OAuth账号
     */
    @DeleteMapping("/me/oauth/{provider}")
    public R<Void> unbindOAuth(@AuthenticationPrincipal LoginUser loginUser,
                               @PathVariable String provider) {
        userService.unbindOAuth(loginUser.getUser().getId(), provider);
        return R.ok();
    }
}
