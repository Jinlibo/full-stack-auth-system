package com.product.app.service;

import com.product.app.dto.PermissionRequest;
import com.product.app.entity.AppPermission;

import java.util.List;

/**
 * 权限服务接口，定义权限的查询、新增、修改和删除操作。
 */
public interface AppPermissionService {

    /**
     * 查询所有权限，按排序字段升序返回。
     *
     * @return 权限列表
     */
    List<AppPermission> listAllPermissions();

    /**
     * 新增权限。
     *
     * @param request 权限创建请求
     * @return 创建成功的权限实体
     */
    AppPermission createPermission(PermissionRequest request);

    /**
     * 修改指定权限信息，仅更新请求中非 null 的字段。
     *
     * @param id      权限 ID
     * @param request 权限更新请求
     */
    void updatePermission(Long id, PermissionRequest request);

    /**
     * 删除指定权限，权限不存在时抛出业务异常。
     *
     * @param id 权限 ID
     */
    void deletePermission(Long id);
}
