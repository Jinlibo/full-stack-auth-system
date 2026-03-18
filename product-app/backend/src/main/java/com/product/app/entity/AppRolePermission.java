package com.product.app.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 角色权限关联实体类，对应数据库表 app_role_permission，维护角色与权限的多对多关系。
 */
@Data
@TableName("app_role_permission")
public class AppRolePermission {

    /** 角色 ID */
    private Long roleId;

    /** 权限 ID */
    private Long permissionId;
}
