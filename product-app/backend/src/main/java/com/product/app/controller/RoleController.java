package com.product.app.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.product.app.common.PageQuery;
import com.product.app.common.R;
import com.product.app.dto.RoleRequest;
import com.product.app.entity.AppRole;
import com.product.app.service.AppRoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 角色管理控制器，提供角色的分页查询、新增、修改、删除及角色权限/用户角色分配接口。
 */
@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
public class RoleController {

    private final AppRoleService roleService;

    /**
     * 分页查询角色列表，支持按角色名或角色标识关键词搜索。
     * 需要 role:list 权限。
     *
     * @param query 分页查询参数
     * @return 角色分页数据
     */
    @GetMapping
    @PreAuthorize("hasAuthority('role:list')")
    public R<IPage<AppRole>> pageRoles(PageQuery query) {
        return R.ok(roleService.pageRoles(query));
    }

    /**
     * 查询所有角色（不分页），按排序字段升序排列。
     * 需要 role:list 权限。
     *
     * @return 角色列表
     */
    @GetMapping("/all")
    @PreAuthorize("hasAuthority('role:list')")
    public R<List<AppRole>> listAllRoles() {
        return R.ok(roleService.listAllRoles());
    }

    /**
     * 查询指定角色已关联的权限 ID 列表。
     * 需要 role:list 权限。
     *
     * @param id 角色 ID
     * @return 权限 ID 列表
     */
    @GetMapping("/{id}/permissions")
    @PreAuthorize("hasAuthority('role:list')")
    public R<List<Long>> getRolePermissions(@PathVariable Long id) {
        return R.ok(roleService.getRolePermissions(id));
    }

    /**
     * 新增角色。
     * 需要 role:manage 权限。
     *
     * @param request 角色创建请求
     * @return 创建成功的角色实体
     */
    @PostMapping
    @PreAuthorize("hasAuthority('role:manage')")
    public R<AppRole> createRole(@RequestBody RoleRequest request) {
        return R.ok(roleService.createRole(request));
    }

    /**
     * 修改指定角色信息。
     * 需要 role:manage 权限。
     *
     * @param id      角色 ID
     * @param request 角色更新请求
     * @return 空数据成功响应
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('role:manage')")
    public R<Void> updateRole(@PathVariable Long id, @RequestBody RoleRequest request) {
        roleService.updateRole(id, request);
        return R.ok();
    }

    /**
     * 删除指定角色，同时清除该角色的权限关联数据。
     * 需要 role:manage 权限。
     *
     * @param id 角色 ID
     * @return 空数据成功响应
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('role:manage')")
    public R<Void> deleteRole(@PathVariable Long id) {
        roleService.deleteRole(id);
        return R.ok();
    }

    /**
     * 为指定角色分配权限（先清空后重新绑定）。
     * 需要 role:manage 权限。
     *
     * @param id   角色 ID
     * @param body 请求体，包含 permissionIds 权限 ID 列表
     * @return 空数据成功响应
     */
    @PostMapping("/{id}/permissions")
    @PreAuthorize("hasAuthority('role:manage')")
    public R<Void> assignPermissions(@PathVariable Long id, @RequestBody Map<String, List<Long>> body) {
        roleService.assignPermissions(id, body.get("permissionIds"));
        return R.ok();
    }

    /**
     * 查询指定用户已关联的角色 ID 列表。
     * 需要 role:list 权限。
     *
     * @param userId 用户 ID
     * @return 角色 ID 列表
     */
    @GetMapping("/users/{userId}")
    @PreAuthorize("hasAuthority('role:list')")
    public R<List<Long>> getUserRoles(@PathVariable Long userId) {
        return R.ok(roleService.getUserRoles(userId));
    }

    /**
     * 为指定用户分配角色（先清空后重新绑定）。
     * 需要 role:manage 权限。
     *
     * @param userId 用户 ID
     * @param body   请求体，包含 roleIds 角色 ID 列表
     * @return 空数据成功响应
     */
    @PostMapping("/users/{userId}")
    @PreAuthorize("hasAuthority('role:manage')")
    public R<Void> assignUserRoles(@PathVariable Long userId, @RequestBody Map<String, List<Long>> body) {
        roleService.assignUserRoles(userId, body.get("roleIds"));
        return R.ok();
    }
}
