package com.product.app.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户 OAuth2 绑定实体类，对应数据库表 app_user_oauth，
 * 记录本地用户与第三方 OAuth2 账号的绑定关系。
 */
@Data
@TableName("app_user_oauth")
public class AppUserOauth {

    /** 主键 ID，自增 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关联的本地用户 ID */
    private Long userId;

    /** OAuth2 提供商标识，如 auth-platform */
    private String oauthProvider;

    /** OAuth2 提供商侧的用户唯一标识（sub） */
    private String oauthUid;

    /** OAuth2 提供商侧的用户名 */
    private String oauthUsername;

    /** OAuth2 提供商侧的用户头像地址 */
    private String oauthAvatar;

    /** OAuth2 访问令牌（当前未持久化到数据库） */
    private String accessToken;

    /** OAuth2 刷新令牌（当前未持久化到数据库） */
    private String refreshToken;

    /** OAuth2 访问令牌过期时间 */
    private LocalDateTime tokenExpireAt;

    /** 创建时间，插入时自动填充 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /** 更新时间，插入和更新时自动填充 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
