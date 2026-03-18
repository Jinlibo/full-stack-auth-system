package com.product.app.dto;

import lombok.Data;

/**
 * 新增或修改权限请求 DTO，包含权限名称、权限标识、类型、路径等信息。
 */
@Data
public class PermissionRequest {

    /** 权限名称 */
    private String permissionName;

    /** 权限标识键，如 user:list */
    private String permissionKey;

    /** 父级权限 ID，顶级权限为 null */
    private Long parentId;

    /** 权限类型：1=菜单，2=按钮，3=API */
    private Integer type;

    /** 权限对应的前端路由路径或接口路径 */
    private String path;

    /** 排序序号 */
    private Integer sortOrder;

    /** 状态：1=启用，0=禁用 */
    private Integer status;
}
