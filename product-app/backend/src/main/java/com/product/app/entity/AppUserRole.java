package com.product.app.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 用户角色关联实体类，对应数据库表 app_user_role，维护用户与角色的多对多关系。
 */
@Data
@TableName("app_user_role")
public class AppUserRole {

    /** 用户 ID */
    private Long userId;

    /** 角色 ID */
    private Long roleId;
}
