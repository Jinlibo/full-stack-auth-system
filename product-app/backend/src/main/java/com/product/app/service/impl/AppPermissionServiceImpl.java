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

/**
 * 权限服务实现类，提供权限的查询、新增、修改和删除业务逻辑。
 */
@Service
@RequiredArgsConstructor
public class AppPermissionServiceImpl implements AppPermissionService {

    private final AppPermissionMapper permissionMapper;

    /**
     * 查询所有权限，按排序字段升序返回。
     *
     * @return 权限列表
     */
    @Override
    public List<AppPermission> listAllPermissions() {
        return permissionMapper.selectList(new LambdaQueryWrapper<AppPermission>()
                .orderByAsc(AppPermission::getSortOrder));
    }

    /**
     * 新增权限，状态默认为 1（启用）。
     *
     * @param request 权限创建请求
     * @return 创建成功的权限实体
     */
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

    /**
     * 修改指定权限信息，仅更新请求中非 null 的字段。
     * 权限不存在时抛出 404 业务异常。
     *
     * @param id      权限 ID
     * @param request 权限更新请求
     */
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

    /**
     * 删除指定权限，权限不存在时抛出 404 业务异常。
     *
     * @param id 权限 ID
     */
    @Override
    public void deletePermission(Long id) {
        if (permissionMapper.selectById(id) == null) throw new BusinessException(404, "权限不存在");
        permissionMapper.deleteById(id);
    }
}
