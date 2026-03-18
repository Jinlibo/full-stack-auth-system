package com.product.app.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.product.app.common.PageQuery;
import com.product.app.dto.RoleRequest;
import com.product.app.entity.AppRole;

import java.util.List;

/**
 * 角色服务接口，定义角色的分页查询、新增、修改、删除及角色权限/用户角色分配操作。
 */
public interface AppRoleService {

    /**
     * 分页查询角色，支持按角色名或角色标识关键词搜索。
     *
     * @param query 分页查询参数
     * @return 角色分页数据
     */
    IPage<AppRole> pageRoles(PageQuery query);

    /**
     * 查询所有角色（不分页），按排序字段升序返回。
     *
     * @return 角色列表
     */
    List<AppRole> listAllRoles();

    /**
     * 新增角色。
     *
     * @param request 角色创建请求
     * @return 创建成功的角色实体
     */
    AppRole createRole(RoleRequest request);

    /**
     * 修改指定角色信息，仅更新请求中非 null 的字段。
     *
     * @param id      角色 ID
     * @param request 角色更新请求
     */
    void updateRole(Long id, RoleRequest request);

    /**
     * 删除指定角色，同时清除该角色的权限关联数据。
     *
     * @param id 角色 ID
     */
    void deleteRole(Long id);

    /**
     * 查询指定角色已关联的权限 ID 列表。
     *
     * @param roleId 角色 ID
     * @return 权限 ID 列表
     */
    List<Long> getRolePermissions(Long roleId);

    /**
     * 为指定角色分配权限（先清空后重新绑定）。
     *
     * @param roleId        角色 ID
     * @param permissionIds 权限 ID 列表
     */
    void assignPermissions(Long roleId, List<Long> permissionIds);

    /**
     * 查询指定用户已关联的角色 ID 列表。
     *
     * @param userId 用户 ID
     * @return 角色 ID 列表
     */
    List<Long> getUserRoles(Long userId);

    /**
     * 为指定用户分配角色（先清空后重新绑定）。
     *
     * @param userId  用户 ID
     * @param roleIds 角色 ID 列表
     */
    void assignUserRoles(Long userId, List<Long> roleIds);
}
