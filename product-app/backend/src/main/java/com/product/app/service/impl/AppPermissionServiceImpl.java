package com.product.app.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.product.app.common.BusinessException;
import com.product.app.dto.PermissionRequest;
import com.product.app.entity.AppPermission;
import com.product.app.mapper.AppPermissionMapper;
import com.product.app.service.AppPermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AppPermissionServiceImpl implements AppPermissionService {

    private final AppPermissionMapper permissionMapper;

    @Override
    public List<AppPermission> listAllPermissions() {
        return permissionMapper.selectList(new LambdaQueryWrapper<AppPermission>()
                .orderByAsc(AppPermission::getSortOrder));
    }

    @Override
    public AppPermission createPermission(PermissionRequest request) {
        AppPermission permission = new AppPermission();
        permission.setPermissionName(request.getPermissionName());
        permission.setPermissionKey(request.getPermissionKey());
        permission.setParentId(request.getParentId());
        permission.setType(request.getType());
        permission.setPath(request.getPath());
        permission.setSortOrder(request.getSortOrder());
        permission.setStatus(request.getStatus() != null ? request.getStatus() : 1);
        permissionMapper.insert(permission);
        return permission;
    }

    @Override
    public void updatePermission(Long id, PermissionRequest request) {
        AppPermission permission = permissionMapper.selectById(id);
        if (permission == null) throw new BusinessException(404, "权限不存在");
        if (request.getPermissionName() != null) permission.setPermissionName(request.getPermissionName());
        if (request.getPermissionKey() != null) permission.setPermissionKey(request.getPermissionKey());
        if (request.getParentId() != null) permission.setParentId(request.getParentId());
        if (request.getType() != null) permission.setType(request.getType());
        if (request.getPath() != null) permission.setPath(request.getPath());
        if (request.getSortOrder() != null) permission.setSortOrder(request.getSortOrder());
        if (request.getStatus() != null) permission.setStatus(request.getStatus());
        permissionMapper.updateById(permission);
    }

    @Override
    public void deletePermission(Long id) {
        if (permissionMapper.selectById(id) == null) throw new BusinessException(404, "权限不存在");
        permissionMapper.deleteById(id);
    }
}
