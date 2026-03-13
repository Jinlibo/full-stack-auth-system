package com.auth.platform.service;

import com.auth.platform.dto.PermissionRequest;
import com.auth.platform.entity.SysPermission;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;
import java.util.Map;

public interface SysPermissionService extends IService<SysPermission> {
    List<Map<String, Object>> getPermissionTree();

    void createPermission(PermissionRequest request);

    void updatePermission(Long id, PermissionRequest request);

    void deletePermission(Long id);
}
