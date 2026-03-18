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

/**
 * 角色服务实现类，提供角色的分页查询、新增、修改、删除及角色权限/用户角色分配业务逻辑。
 */
@Service
@RequiredArgsConstructor
public class AppRoleServiceImpl implements AppRoleService {

    private final AppRoleMapper roleMapper;
    private final AppRolePermissionMapper rolePermissionMapper;
    private final AppUserRoleMapper userRoleMapper;

    /**
     * 分页查询角色，支持按角色名或角色标识关键词模糊搜索，按创建时间倒序排列。
     *
     * @param query 分页查询参数
     * @return 角色分页数据
     */
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

    /**
     * 查询所有角色（不分页），按排序字段升序返回。
     *
     * @return 角色列表
     */
    @Override
    public List<AppRole> listAllRoles() {
        return roleMapper.selectList(new LambdaQueryWrapper<AppRole>()
                .orderByAsc(AppRole::getSortOrder));
    }

    /**
     * 新增角色，状态默认为 1（启用）。
     *
     * @param request 角色创建请求
     * @return 创建成功的角色实体
     */
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

    /**
     * 修改指定角色信息，仅更新请求中非 null 的字段。
     * 角色不存在时抛出 404 业务异常。
     *
     * @param id      角色 ID
     * @param request 角色更新请求
     */
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

    /**
     * 删除指定角色，同时同步删除该角色的所有权限关联记录。
     * 角色不存在时抛出 404 业务异常。
     *
     * @param id 角色 ID
     */
    @Override
    public void deleteRole(Long id) {
        if (roleMapper.selectById(id) == null) throw new BusinessException(404, "角色不存在");
        roleMapper.deleteById(id);
        // 同步删除角色权限关联记录
        rolePermissionMapper.deleteByRoleId(id);
    }

    /**
     * 查询指定角色已关联的权限 ID 列表。
     *
     * @param roleId 角色 ID
     * @return 权限 ID 列表
     */
    @Override
    public List<Long> getRolePermissions(Long roleId) {
        return rolePermissionMapper.selectList(
                new LambdaQueryWrapper<AppRolePermission>()
                        .eq(AppRolePermission::getRoleId, roleId))
                .stream()
                .map(AppRolePermission::getPermissionId)
                .toList();
    }

    /**
     * 为指定角色分配权限，采用先全部删除再批量插入的方式。
     *
     * @param roleId        角色 ID
     * @param permissionIds 新的权限 ID 列表
     */
    @Override
    @Transactional
    public void assignPermissions(Long roleId, List<Long> permissionIds) {
        // 先删除旧关联，再插入新关联
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

    /**
     * 查询指定用户已关联的角色 ID 列表。
     *
     * @param userId 用户 ID
     * @return 角色 ID 列表
     */
    @Override
    public List<Long> getUserRoles(Long userId) {
        return userRoleMapper.selectList(
                new LambdaQueryWrapper<AppUserRole>()
                        .eq(AppUserRole::getUserId, userId))
                .stream()
                .map(AppUserRole::getRoleId)
                .toList();
    }

    /**
     * 为指定用户分配角色，采用先全部删除再批量插入的方式。
     *
     * @param userId  用户 ID
     * @param roleIds 新的角色 ID 列表
     */
    @Override
    @Transactional
    public void assignUserRoles(Long userId, List<Long> roleIds) {
        // 先删除旧关联，再插入新关联
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
