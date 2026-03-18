package com.auth.platform.service;

import com.auth.platform.common.PageQuery;
import com.auth.platform.dto.RoleRequest;
import com.auth.platform.entity.SysRole;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * 系统角色管理服务接口
 * <p>继承 MyBatis-Plus 的 {@link IService}，获得基础 CRUD 能力。
 * 扩展了角色的分页查询、权限关联管理等功能。</p>
 */
public interface SysRoleService extends IService<SysRole> {

    /**
     * 分页查询角色列表
     * <p>支持按角色名称和角色标识关键字模糊搜索，按排序序号升序排列。</p>
     *
     * @param query 分页查询参数（pageNum、pageSize、keyword）
     * @return 分页角色列表
     */
    IPage<SysRole> pageRoles(PageQuery query);

    /**
     * 查询所有启用状态的角色列表（不分页）
     * <p>用于下拉框等需要展示全量角色的场景，仅返回 status=1 的角色，按排序序号升序。</p>
     *
     * @return 所有启用角色的列表
     */
    List<SysRole> listAllRoles();

    /**
     * 创建角色（同时关联权限节点）
     * <p>roleKey 必须全局唯一，重复时抛出业务异常。创建时可一并指定权限节点列表。</p>
     *
     * @param request 角色创建请求（roleName、roleKey、permissionIds 等）
     */
    void createRole(RoleRequest request);

    /**
     * 更新角色信息（同时更新权限关联）
     * <p>若请求中包含 permissionIds，则全量替换该角色的权限关联。角色不存在时抛出 404 异常。</p>
     *
     * @param roleId  要更新的角色 ID
     * @param request 更新请求
     */
    void updateRole(Long roleId, RoleRequest request);

    /**
     * 删除角色（同时清除权限关联）
     * <p>禁止删除 id=1 的超级管理员角色，防止系统失去管理能力。</p>
     *
     * @param roleId 要删除的角色 ID
     */
    void deleteRole(Long roleId);

    /**
     * 获取指定角色已关联的权限节点 ID 列表
     * <p>用于角色编辑页面回显已勾选的权限节点。</p>
     *
     * @param roleId 角色 ID
     * @return 该角色关联的权限节点 ID 列表
     */
    List<Long> getRolePermissionIds(Long roleId);
}
