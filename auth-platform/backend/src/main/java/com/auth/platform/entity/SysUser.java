package com.auth.platform.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 系统用户实体类
 * <p>对应数据库表 sys_user，存储系统中所有用户的基本信息和账号状态。
 * 使用 MyBatis-Plus 的逻辑删除功能（@TableLogic），删除操作仅将 deleted 字段置为 1，
 * 不会物理删除记录，所有查询自动过滤 deleted=1 的数据。</p>
 */
@Data
@TableName("sys_user")
public class SysUser {
    /** 用户主键 ID，数据库自增 */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 用户名（登录名），全局唯一 */
    private String username;
    /** BCrypt 加密后的登录密码，不可明文存储 */
    private String password;
    /** 用户邮箱 */
    private String email;
    /** 用户手机号 */
    private String phone;
    /** 用户昵称（显示名称） */
    private String nickname;
    /** 用户头像图片地址 */
    private String avatar;
    /** 账号状态：1=正常，0=禁用；禁用时用户无法登录 */
    private Integer status;
    /** 逻辑删除标志：0=未删除，1=已删除；MyBatis-Plus 查询时自动过滤 deleted=1 的记录 */
    @TableLogic
    private Integer deleted;
    /** 记录创建时间，由 MyBatis-Plus 自动填充 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    /** 记录最后更新时间，由 MyBatis-Plus 自动填充 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
