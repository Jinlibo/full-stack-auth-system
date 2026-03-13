package com.auth.platform.service;

import com.auth.platform.common.PageQuery;
import com.auth.platform.dto.RoleRequest;
import com.auth.platform.entity.SysRole;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface SysRoleService extends IService<SysRole> {
    IPage<SysRole> pageRoles(PageQuery query);

    List<SysRole> listAllRoles();

    void createRole(RoleRequest request);

    void updateRole(Long roleId, RoleRequest request);

    void deleteRole(Long roleId);

    List<Long> getRolePermissionIds(Long roleId);
}
