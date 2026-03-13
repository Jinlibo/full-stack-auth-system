package com.auth.platform.controller;

import com.auth.platform.common.PageQuery;
import com.auth.platform.common.R;
import com.auth.platform.dto.RoleRequest;
import com.auth.platform.entity.SysRole;
import com.auth.platform.service.SysRoleService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
public class SysRoleController {

    private final SysRoleService roleService;

    @GetMapping
    @PreAuthorize("hasAuthority('system:role:query')")
    public R<IPage<SysRole>> pageRoles(PageQuery query) {
        return R.ok(roleService.pageRoles(query));
    }

    @GetMapping("/all")
    public R<List<SysRole>> listAllRoles() {
        return R.ok(roleService.listAllRoles());
    }

    @GetMapping("/{id}/permissions")
    public R<List<Long>> getRolePermissions(@PathVariable Long id) {
        return R.ok(roleService.getRolePermissionIds(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('system:role:add')")
    public R<Void> createRole(@Valid @RequestBody RoleRequest request) {
        roleService.createRole(request);
        return R.ok();
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('system:role:edit')")
    public R<Void> updateRole(@PathVariable Long id, @Valid @RequestBody RoleRequest request) {
        roleService.updateRole(id, request);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('system:role:delete')")
    public R<Void> deleteRole(@PathVariable Long id) {
        roleService.deleteRole(id);
        return R.ok();
    }
}
