package com.product.app.controller;

import com.product.app.common.R;
import com.product.app.dto.PermissionRequest;
import com.product.app.entity.AppPermission;
import com.product.app.service.AppPermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/permissions")
@RequiredArgsConstructor
public class PermissionController {

    private final AppPermissionService permissionService;

    @GetMapping
    @PreAuthorize("hasAuthority('permission:list')")
    public R<List<AppPermission>> listAllPermissions() {
        return R.ok(permissionService.listAllPermissions());
    }

    @PostMapping
    @PreAuthorize("hasAuthority('permission:manage')")
    public R<AppPermission> createPermission(@RequestBody PermissionRequest request) {
        return R.ok(permissionService.createPermission(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('permission:manage')")
    public R<Void> updatePermission(@PathVariable Long id, @RequestBody PermissionRequest request) {
        permissionService.updatePermission(id, request);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('permission:manage')")
    public R<Void> deletePermission(@PathVariable Long id) {
        permissionService.deletePermission(id);
        return R.ok();
    }
}
