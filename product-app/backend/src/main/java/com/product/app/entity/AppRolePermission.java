package com.product.app.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("app_role_permission")
public class AppRolePermission {
    private Long roleId;
    private Long permissionId;
}
