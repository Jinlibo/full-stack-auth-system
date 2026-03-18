package com.product.app.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 权限实体类，对应数据库表 app_permission，存储系统菜单、按钮及 API 级别的权限信息。
 */
@Data
@TableName("app_permission")
public class AppPermission {

    /** 主键 ID，自增 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 权限名称，如"用户列表" */
    private String permissionName;

    /** 权限标识键，如 user:list */
    private String permissionKey;

    /** 父级权限 ID，顶级权限为 null */
    private Long parentId;

    /** 权限类型：1=菜单，2=按钮，3=API */
    private Integer type;

    /** 路由路径或接口路径 */
    private String path;

    /** 菜单图标 */
    private String icon;

    /** 排序序号 */
    private Integer sortOrder;

    /** 状态：1=启用，0=禁用 */
    private Integer status;

    /** 创建时间，插入时自动填充 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /** 更新时间，插入和更新时自动填充 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
