package com.auth.platform.service.impl;

import com.auth.platform.common.BusinessException;
import com.auth.platform.common.PageQuery;
import com.auth.platform.dto.RoleRequest;
import com.auth.platform.entity.SysRole;
import com.auth.platform.entity.SysRolePermission;
import com.auth.platform.mapper.SysRoleMapper;
import com.auth.platform.mapper.SysRolePermissionMapper;
import com.auth.platform.service.SysRoleService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SysRoleServiceImpl extends ServiceImpl<SysRoleMapper, SysRole> implements SysRoleService {

    private final SysRoleMapper roleMapper;
    private final SysRolePermissionMapper rolePermissionMapper;

    @Override
    public IPage<SysRole> pageRoles(PageQuery query) {
        Page<SysRole> page = new Page<>(query.getPageNum(), query.getPageSize());
        LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(query.getKeyword())) {
            wrapper.like(SysRole::getRoleName, query.getKeyword())
                    .or().like(SysRole::getRoleKey, query.getKeyword());
        }
        wrapper.orderByAsc(SysRole::getSortOrder);
        return roleMapper.selectPage(page, wrapper);
    }

    @Override
    public List<SysRole> listAllRoles() {
        return roleMapper.selectList(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getStatus, 1)
                .orderByAsc(SysRole::getSortOrder));
    }

    @Override
    @Transactional
    public void createRole(RoleRequest request) {
        Long count = roleMapper.selectCount(
                new LambdaQueryWrapper<SysRole>().eq(SysRole::getRoleKey, request.getRoleKey()));
        if (count > 0) throw new BusinessException(400, "角色标识已存在");

        SysRole role = new SysRole();
        role.setRoleName(request.getRoleName());
        role.setRoleKey(request.getRoleKey());
        role.setSortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0);
        role.setRemark(request.getRemark());
        role.setStatus(1);
        roleMapper.insert(role);

        if (!CollectionUtils.isEmpty(request.getPermissionIds())) {
            request.getPermissionIds().forEach(permId -> {
                SysRolePermission rp = new SysRolePermission();
                rp.setRoleId(role.getId());
                rp.setPermissionId(permId);
                rolePermissionMapper.insert(rp);
            });
        }
    }

    @Override
    @Transactional
    public void updateRole(Long roleId, RoleRequest request) {
        SysRole role = roleMapper.selectById(roleId);
        if (role == null) throw new BusinessException(404, "角色不存在");

        role.setRoleName(request.getRoleName());
        role.setRoleKey(request.getRoleKey());
        if (request.getSortOrder() != null) role.setSortOrder(request.getSortOrder());
        role.setRemark(request.getRemark());
        roleMapper.updateById(role);

        if (request.getPermissionIds() != null) {
            rolePermissionMapper.deleteByRoleId(roleId);
            request.getPermissionIds().forEach(permId -> {
                SysRolePermission rp = new SysRolePermission();
                rp.setRoleId(roleId);
                rp.setPermissionId(permId);
                rolePermissionMapper.insert(rp);
            });
        }
    }

    @Override
    public void deleteRole(Long roleId) {
        if (roleId == 1L) throw new BusinessException("不能删除超级管理员角色");
        roleMapper.deleteById(roleId);
        rolePermissionMapper.deleteByRoleId(roleId);
    }

    @Override
    public List<Long> getRolePermissionIds(Long roleId) {
        return rolePermissionMapper.selectList(
                        new LambdaQueryWrapper<SysRolePermission>().eq(SysRolePermission::getRoleId, roleId))
                .stream().map(SysRolePermission::getPermissionId).toList();
    }
}
