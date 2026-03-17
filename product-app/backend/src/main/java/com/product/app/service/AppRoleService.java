package com.product.app.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.product.app.common.PageQuery;
import com.product.app.dto.RoleRequest;
import com.product.app.entity.AppRole;

import java.util.List;

public interface AppRoleService {

    IPage<AppRole> pageRoles(PageQuery query);

    List<AppRole> listAllRoles();

    AppRole createRole(RoleRequest request);

    void updateRole(Long id, RoleRequest request);

    void deleteRole(Long id);

    List<Long> getRolePermissions(Long roleId);

    void assignPermissions(Long roleId, List<Long> permissionIds);

    List<Long> getUserRoles(Long userId);

    void assignUserRoles(Long userId, List<Long> roleIds);
}
