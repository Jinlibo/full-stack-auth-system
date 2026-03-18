package com.auth.platform.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 系统权限实体类
 * <p>对应数据库表 sys_permission，以树形结构存储系统中的菜单、按钮和 API 权限节点。
 * 通过 parentId 字段建立父子层级关系，parentId=0 表示顶级节点。</p>
 */
@Data
@TableName("sys_permission")
public class SysPermission {
    /** 权限主键 ID，数据库自增 */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 权限显示名称，如"用户管理"、"新增用户" */
    private String permissionName;
    /** 权限唯一标识符，如"system:user:add"，用于 @PreAuthorize 注解引用 */
    private String permissionKey;
    /** 父节点 ID，值为 0 时表示顶级权限节点 */
    private Long parentId;
    /** 权限类型：1=菜单，2=按钮，3=API 接口 */
    private Integer type;
    /** 路由路径或接口 URL（菜单类型使用前端路由地址） */
    private String path;
    /** 菜单图标名称，仅菜单类型权限使用 */
    private String icon;
    /** 同级节点排序序号，数值越小越靠前 */
    private Integer sortOrder;
    /** 权限状态：1=正常，0=禁用 */
    private Integer status;
    /** 记录创建时间，由 MyBatis-Plus 自动填充 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    /** 记录最后更新时间，由 MyBatis-Plus 自动填充 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
