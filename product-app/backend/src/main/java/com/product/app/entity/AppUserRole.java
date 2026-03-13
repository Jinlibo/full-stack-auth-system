package com.product.app.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("app_user_role")
public class AppUserRole {
    private Long userId;
    private Long roleId;
}
