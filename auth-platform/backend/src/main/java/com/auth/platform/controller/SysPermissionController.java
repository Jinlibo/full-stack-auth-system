package com.auth.platform.controller;

import com.auth.platform.common.R;
import com.auth.platform.dto.PermissionRequest;
import com.auth.platform.service.SysPermissionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/permissions")
@RequiredArgsConstructor
public class SysPermissionController {

    private final SysPermissionService permissionService;

    @GetMapping("/tree")
    public R<List<Map<String, Object>>> getPermissionTree() {
        return R.ok(permissionService.getPermissionTree());
    }

    @PostMapping
    @PreAuthorize("hasAuthority('system:permission')")
    public R<Void> createPermission(@Valid @RequestBody PermissionRequest request) {
        permissionService.createPermission(request);
        return R.ok();
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('system:permission')")
    public R<Void> updatePermission(@PathVariable Long id, @Valid @RequestBody PermissionRequest request) {
        permissionService.updatePermission(id, request);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('system:permission')")
    public R<Void> deletePermission(@PathVariable Long id) {
        permissionService.deletePermission(id);
        return R.ok();
    }
}
