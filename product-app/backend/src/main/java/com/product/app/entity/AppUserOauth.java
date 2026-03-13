package com.product.app.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("app_user_oauth")
public class AppUserOauth {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String oauthProvider;
    private String oauthUid;
    private String oauthUsername;
    private String oauthAvatar;
    private String accessToken;
    private String refreshToken;
    private LocalDateTime tokenExpireAt;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
