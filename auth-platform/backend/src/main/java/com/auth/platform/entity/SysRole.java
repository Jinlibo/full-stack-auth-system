package com.auth.platform.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 系统角色实体类
 * <p>对应数据库表 sys_role，存储系统中定义的所有角色信息。
 * 角色通过 sys_user_role 中间表与用户关联，通过 sys_role_permission 中间表与权限关联。</p>
 */
@Data
@TableName("sys_role")
public class SysRole {
    /** 角色主键 ID，数据库自增 */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 角色显示名称，如"超级管理员"、"普通用户" */
    private String roleName;
    /** 角色唯一标识符，如"SUPER_ADMIN"、"USER"，用于 Spring Security 权限控制 */
    private String roleKey;
    /** 排序序号，数值越小越靠前 */
    private Integer sortOrder;
    /** 角色状态：1=正常，0=禁用 */
    private Integer status;
    /** 角色备注说明 */
    private String remark;
    /** 记录创建时间，由 MyBatis-Plus 自动填充 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    /** 记录最后更新时间，由 MyBatis-Plus 自动填充 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
