package com.product.app.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.product.app.common.BusinessException;
import com.product.app.common.PageQuery;
import com.product.app.dto.RoleRequest;
import com.product.app.entity.AppRole;
import com.product.app.entity.AppRolePermission;
import com.product.app.entity.AppUserRole;
import com.product.app.mapper.AppRoleMapper;
import com.product.app.mapper.AppRolePermissionMapper;
import com.product.app.mapper.AppUserRoleMapper;
import com.product.app.service.AppRoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AppRoleServiceImpl implements AppRoleService {

    private final AppRoleMapper roleMapper;
    private final AppRolePermissionMapper rolePermissionMapper;
    private final AppUserRoleMapper userRoleMapper;

    @Override
    public IPage<AppRole> pageRoles(PageQuery query) {
        Page<AppRole> page = new Page<>(query.getPageNum(), query.getPageSize());
        LambdaQueryWrapper<AppRole> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(query.getKeyword())) {
            wrapper.like(AppRole::getRoleName, query.getKeyword())
                    .or().like(AppRole::getRoleKey, query.getKeyword());
        }
        wrapper.orderByDesc(AppRole::getCreatedAt);
        return roleMapper.selectPage(page, wrapper);
    }

    @Override
    public List<AppRole> listAllRoles() {
        return roleMapper.selectList(new LambdaQueryWrapper<AppRole>()
                .orderByAsc(AppRole::getSortOrder));
    }

    @Override
    public AppRole createRole(RoleRequest request) {
        AppRole role = new AppRole();
        role.setRoleName(request.getRoleName());
        role.setRoleKey(request.getRoleKey());
        role.setRemark(request.getRemark());
        role.setStatus(request.getStatus() != null ? request.getStatus() : 1);
        roleMapper.insert(role);
        return role;
    }

    @Override
    public void updateRole(Long id, RoleRequest request) {
        AppRole role = roleMapper.selectById(id);
        if (role == null) throw new BusinessException(404, "角色不存在");
        if (request.getRoleName() != null) role.setRoleName(request.getRoleName());
        if (request.getRoleKey() != null) role.setRoleKey(request.getRoleKey());
        if (request.getRemark() != null) role.setRemark(request.getRemark());
        if (request.getStatus() != null) role.setStatus(request.getStatus());
        roleMapper.updateById(role);
    }

    @Override
    public void deleteRole(Long id) {
        if (roleMapper.selectById(id) == null) throw new BusinessException(404, "角色不存在");
        roleMapper.deleteById(id);
        // 同步删除角色权限关联和用户角色关联
        rolePermissionMapper.deleteByRoleId(id);
    }

    @Override
    public List<Long> getRolePermissions(Long roleId) {
        return rolePermissionMapper.selectList(
                new LambdaQueryWrapper<AppRolePermission>()
                        .eq(AppRolePermission::getRoleId, roleId))
                .stream()
                .map(AppRolePermission::getPermissionId)
                .toList();
    }

    @Override
    @Transactional
    public void assignPermissions(Long roleId, List<Long> permissionIds) {
        // 先删后加
        rolePermissionMapper.deleteByRoleId(roleId);
        if (permissionIds != null && !permissionIds.isEmpty()) {
            for (Long permId : permissionIds) {
                AppRolePermission rp = new AppRolePermission();
                rp.setRoleId(roleId);
                rp.setPermissionId(permId);
                rolePermissionMapper.insert(rp);
            }
        }
    }

    @Override
    public List<Long> getUserRoles(Long userId) {
        return userRoleMapper.selectList(
                new LambdaQueryWrapper<AppUserRole>()
                        .eq(AppUserRole::getUserId, userId))
                .stream()
                .map(AppUserRole::getRoleId)
                .toList();
    }

    @Override
    @Transactional
    public void assignUserRoles(Long userId, List<Long> roleIds) {
        // 先删后加
        userRoleMapper.deleteByUserId(userId);
        if (roleIds != null && !roleIds.isEmpty()) {
            for (Long roleId : roleIds) {
                AppUserRole ur = new AppUserRole();
                ur.setUserId(userId);
                ur.setRoleId(roleId);
                userRoleMapper.insert(ur);
            }
        }
    }
}
