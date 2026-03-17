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

@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
public class RoleController {

    private final AppRoleService roleService;

    @GetMapping
    @PreAuthorize("hasAuthority('role:list')")
    public R<IPage<AppRole>> pageRoles(PageQuery query) {
        return R.ok(roleService.pageRoles(query));
    }

    @GetMapping("/all")
    @PreAuthorize("hasAuthority('role:list')")
    public R<List<AppRole>> listAllRoles() {
        return R.ok(roleService.listAllRoles());
    }

    @GetMapping("/{id}/permissions")
    @PreAuthorize("hasAuthority('role:list')")
    public R<List<Long>> getRolePermissions(@PathVariable Long id) {
        return R.ok(roleService.getRolePermissions(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('role:manage')")
    public R<AppRole> createRole(@RequestBody RoleRequest request) {
        return R.ok(roleService.createRole(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('role:manage')")
    public R<Void> updateRole(@PathVariable Long id, @RequestBody RoleRequest request) {
        roleService.updateRole(id, request);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('role:manage')")
    public R<Void> deleteRole(@PathVariable Long id) {
        roleService.deleteRole(id);
        return R.ok();
    }

    @PostMapping("/{id}/permissions")
    @PreAuthorize("hasAuthority('role:manage')")
    public R<Void> assignPermissions(@PathVariable Long id, @RequestBody Map<String, List<Long>> body) {
        roleService.assignPermissions(id, body.get("permissionIds"));
        return R.ok();
    }

    @GetMapping("/users/{userId}")
    @PreAuthorize("hasAuthority('role:list')")
    public R<List<Long>> getUserRoles(@PathVariable Long userId) {
        return R.ok(roleService.getUserRoles(userId));
    }

    @PostMapping("/users/{userId}")
    @PreAuthorize("hasAuthority('role:manage')")
    public R<Void> assignUserRoles(@PathVariable Long userId, @RequestBody Map<String, List<Long>> body) {
        roleService.assignUserRoles(userId, body.get("roleIds"));
        return R.ok();
    }
}
