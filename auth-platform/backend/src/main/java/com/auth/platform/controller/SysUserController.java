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

/**
 * 系统用户管理控制器
 *
 * <p>提供系统用户（SysUser）的 CRUD REST 接口及当前登录用户的个人信息管理接口。
 * 基础路径：/api/users
 *
 * <p>接口权限控制：
 * <ul>
 *   <li>分页查询 / 单用户查询：需要 {@code system:user:query} 权限</li>
 *   <li>创建用户：需要 {@code system:user:add} 权限</li>
 *   <li>更新用户：需要 {@code system:user:edit} 权限</li>
 *   <li>删除用户：需要 {@code system:user:delete} 权限</li>
 *   <li>获取当前登录用户信息、修改密码、更新个人资料：无需特定权限（已登录即可）</li>
 * </ul>
 *
 * @author auth-platform
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class SysUserController {

    /** 用户管理服务，包含用户 CRUD、密码修改、角色分配等业务逻辑 */
    private final SysUserService userService;

    /**
     * 分页查询用户列表
     *
     * <p>GET /api/users?pageNum=1&pageSize=10&keyword=xxx
     * 支持按用户名、昵称关键字搜索
     *
     * @param query 分页查询参数（pageNum、pageSize、keyword）
     * @return 分页用户信息列表（包含角色信息）
     */
    @GetMapping
    @PreAuthorize("hasAuthority('system:user:query')")
    public R<IPage<UserInfo>> pageUsers(PageQuery query) {
        return R.ok(userService.pageUsers(query));
    }

    /**
     * 获取当前登录用户的详细信息
     *
     * <p>GET /api/users/me
     * 从 SecurityContext 中提取当前认证用户，查询最新的用户信息返回。
     * 无需特定权限，所有已登录用户均可访问。
     *
     * @param loginUser 当前登录用户（由 Spring Security 自动注入）
     * @return 当前用户的详细信息（含角色和权限列表）
     */
    @GetMapping("/me")
    public R<UserInfo> getCurrentUser(@AuthenticationPrincipal LoginUser loginUser) {
        return R.ok(userService.getUserInfo(loginUser.getUser().getId()));
    }

    /**
     * 根据用户 ID 查询用户详细信息
     *
     * <p>GET /api/users/{id}
     *
     * @param id 用户 ID（路径变量）
     * @return 指定用户的详细信息
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('system:user:query')")
    public R<UserInfo> getUser(@PathVariable Long id) {
        return R.ok(userService.getUserInfo(id));
    }

    /**
     * 创建新用户（管理员操作）
     *
     * <p>POST /api/users
     * 创建用户时需指定角色 ID 列表，后端同步插入用户-角色关联记录。
     *
     * @param request 用户创建请求，@Valid 触发参数校验
     * @return 无 data 的成功响应
     */
    @PostMapping
    @PreAuthorize("hasAuthority('system:user:add')")
    public R<Void> createUser(@Valid @RequestBody UserCreateRequest request) {
        userService.createUser(request);
        return R.ok();
    }

    /**
     * 更新用户信息（管理员操作）
     *
     * <p>PUT /api/users/{id}
     * 可更新用户昵称、邮箱、手机号、头像、状态及角色关联。
     *
     * @param id      要更新的用户 ID（路径变量）
     * @param request 用户更新请求
     * @return 无 data 的成功响应
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('system:user:edit')")
    public R<Void> updateUser(@PathVariable Long id, @RequestBody UserUpdateRequest request) {
        userService.updateUser(id, request);
        return R.ok();
    }

    /**
     * 删除用户（管理员操作）
     *
     * <p>DELETE /api/users/{id}
     * 删除用户同时会删除该用户的所有角色关联记录（sys_user_role 表）。
     *
     * @param id 要删除的用户 ID（路径变量）
     * @return 无 data 的成功响应
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('system:user:delete')")
    public R<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return R.ok();
    }

    /**
     * 当前登录用户修改自己的密码
     *
     * <p>PUT /api/users/me/password
     * 需要提供旧密码进行验证，验证通过后才允许修改为新密码。
     *
     * @param loginUser 当前登录用户（由 Spring Security 自动注入）
     * @param request   密码修改请求（含 oldPassword 和 newPassword），@Valid 触发参数校验
     * @return 无 data 的成功响应
     */
    @PutMapping("/me/password")
    public R<Void> changePassword(@AuthenticationPrincipal LoginUser loginUser,
                                  @Valid @RequestBody PasswordChangeRequest request) {
        userService.changePassword(loginUser.getUser().getId(), request);
        return R.ok();
    }

    /**
     * 当前登录用户更新自己的个人资料
     *
     * <p>PUT /api/users/me/profile
     * 用户可更新自己的昵称、头像、邮箱、手机号等基本信息，
     * 不包含密码修改（密码修改使用 /me/password 接口）。
     *
     * @param loginUser 当前登录用户（由 Spring Security 自动注入）
     * @param request   个人资料更新请求
     * @return 无 data 的成功响应
     */
    @PutMapping("/me/profile")
    public R<Void> updateProfile(@AuthenticationPrincipal LoginUser loginUser,
                                 @RequestBody UserUpdateRequest request) {
        userService.updateUser(loginUser.getUser().getId(), request);
        return R.ok();
    }
}
