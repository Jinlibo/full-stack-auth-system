package com.product.app.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户实体类，对应数据库表 app_user，存储系统用户的基本信息。
 * 支持逻辑删除，deleted=1 表示已删除。
 */
@Data
@TableName("app_user")
public class AppUser {

    /** 主键 ID，自增 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户名，唯一 */
    private String username;

    /** BCrypt 加密后的密码，通过 OAuth2 创建的账号可能为 null */
    private String password;

    /** 邮箱地址 */
    private String email;

    /** 手机号码 */
    private String phone;

    /** 用户昵称 */
    private String nickname;

    /** 头像地址 */
    private String avatar;

    /** 账号状态：1=正常，0=禁用 */
    private Integer status;

    /** 逻辑删除标志：0=正常，1=已删除 */
    @TableLogic
    private Integer deleted;

    /** 创建时间，插入时自动填充 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /** 更新时间，插入和更新时自动填充 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
