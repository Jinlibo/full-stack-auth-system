package com.product.app.service;

import com.product.app.dto.PermissionRequest;
import com.product.app.entity.AppPermission;

import java.util.List;

public interface AppPermissionService {

    List<AppPermission> listAllPermissions();

    AppPermission createPermission(PermissionRequest request);

    void updatePermission(Long id, PermissionRequest request);

    void deletePermission(Long id);
}
