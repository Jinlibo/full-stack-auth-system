package com.product.app.dto;

import lombok.Data;

/**
 * 新增或修改角色请求 DTO，包含角色名称、角色标识、备注及状态。
 */
@Data
public class RoleRequest {

    /** 角色名称，如"管理员" */
    private String roleName;

    /** 角色标识键，如 ADMIN */
    private String roleKey;

    /** 角色备注说明 */
    private String remark;

    /** 状态：1=启用，0=禁用 */
    private Integer status;
}
