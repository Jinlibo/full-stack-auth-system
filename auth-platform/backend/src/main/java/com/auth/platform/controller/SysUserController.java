package com.auth.platform.controller;

import com.auth.platform.common.PageQuery;
import com.auth.platform.common.R;
import com.auth.platform.dto.*;
import com.auth.platform.security.LoginUser;
import com.auth.platform.service.SysUserService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class SysUserController {

    private final SysUserService userService;

    @GetMapping
    @PreAuthorize("hasAuthority('system:user:query')")
    public R<IPage<UserInfo>> pageUsers(PageQuery query) {
        return R.ok(userService.pageUsers(query));
    }

    @GetMapping("/me")
    public R<UserInfo> getCurrentUser(@AuthenticationPrincipal LoginUser loginUser) {
        return R.ok(userService.getUserInfo(loginUser.getUser().getId()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('system:user:query')")
    public R<UserInfo> getUser(@PathVariable Long id) {
        return R.ok(userService.getUserInfo(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('system:user:add')")
    public R<Void> createUser(@Valid @RequestBody UserCreateRequest request) {
        userService.createUser(request);
        return R.ok();
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('system:user:edit')")
    public R<Void> updateUser(@PathVariable Long id, @RequestBody UserUpdateRequest request) {
        userService.updateUser(id, request);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('system:user:delete')")
    public R<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return R.ok();
    }

    @PutMapping("/me/password")
    public R<Void> changePassword(@AuthenticationPrincipal LoginUser loginUser,
                                  @Valid @RequestBody PasswordChangeRequest request) {
        userService.changePassword(loginUser.getUser().getId(), request);
        return R.ok();
    }

    @PutMapping("/me/profile")
    public R<Void> updateProfile(@AuthenticationPrincipal LoginUser loginUser,
                                 @RequestBody UserUpdateRequest request) {
        userService.updateUser(loginUser.getUser().getId(), request);
        return R.ok();
    }
}
