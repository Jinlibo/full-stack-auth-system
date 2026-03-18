package com.product.app.controller;

import com.product.app.common.R;
import com.product.app.dto.PermissionRequest;
import com.product.app.entity.AppPermission;
import com.product.app.service.AppPermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 权限管理控制器，提供权限的查询、新增、修改和删除接口，需具备相应权限方可访问。
 */
@RestController
@RequestMapping("/api/permissions")
@RequiredArgsConstructor
public class PermissionController {

    private final AppPermissionService permissionService;

    /**
     * 查询所有权限列表，按排序字段升序排列。
     * 需要 permission:list 权限。
     *
     * @return 权限列表
     */
    @GetMapping
    @PreAuthorize("hasAuthority('permission:list')")
    public R<List<AppPermission>> listAllPermissions() {
        return R.ok(permissionService.listAllPermissions());
    }

    /**
     * 新增权限。
     * 需要 permission:manage 权限。
     *
     * @param request 权限创建请求，包含权限名称、权限标识等
     * @return 创建成功的权限实体
     */
    @PostMapping
    @PreAuthorize("hasAuthority('permission:manage')")
    public R<AppPermission> createPermission(@RequestBody PermissionRequest request) {
        return R.ok(permissionService.createPermission(request));
    }

    /**
     * 修改指定权限信息。
     * 需要 permission:manage 权限。
     *
     * @param id      权限 ID
     * @param request 权限更新请求
     * @return 空数据成功响应
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('permission:manage')")
    public R<Void> updatePermission(@PathVariable Long id, @RequestBody PermissionRequest request) {
        permissionService.updatePermission(id, request);
        return R.ok();
    }

    /**
     * 删除指定权限。
     * 需要 permission:manage 权限。
     *
     * @param id 权限 ID
     * @return 空数据成功响应
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('permission:manage')")
    public R<Void> deletePermission(@PathVariable Long id) {
        permissionService.deletePermission(id);
        return R.ok();
    }
}
