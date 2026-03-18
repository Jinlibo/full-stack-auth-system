package com.auth.platform.service;

import com.auth.platform.dto.PermissionRequest;
import com.auth.platform.entity.SysPermission;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;
import java.util.Map;

/**
 * 系统权限管理服务接口
 * <p>继承 MyBatis-Plus 的 {@link IService}，获得基础 CRUD 能力。
 * 扩展了权限树查询及权限节点的创建、更新、删除功能。</p>
 */
public interface SysPermissionService extends IService<SysPermission> {

    /**
     * 获取完整权限树（树形结构）
     * <p>一次性查询所有权限节点，在内存中递归构建树形结构，
     * 顶级节点的 parentId 为 0，子节点嵌套在 "children" 字段中。</p>
     *
     * @return 递归嵌套的权限树列表，每个节点为 Map 格式（含 id、permissionName、children 等字段）
     */
    List<Map<String, Object>> getPermissionTree();

    /**
     * 创建权限节点
     * <p>permissionKey 必须全局唯一，重复时抛出业务异常。</p>
     *
     * @param request 权限创建请求（permissionName、permissionKey、parentId、type 等）
     */
    void createPermission(PermissionRequest request);

    /**
     * 更新权限节点信息
     * <p>全量更新所有可修改字段，权限节点不存在时抛出 404 异常。</p>
     *
     * @param id      要更新的权限节点 ID
     * @param request 更新内容
     */
    void updatePermission(Long id, PermissionRequest request);

    /**
     * 删除权限节点
     * <p>存在子权限时拒绝删除，防止产生孤儿数据，需先删除所有子节点。</p>
     *
     * @param id 要删除的权限节点 ID
     */
    void deletePermission(Long id);
}
