package com.product.app.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 角色实体类，对应数据库表 app_role，存储系统角色信息。
 */
@Data
@TableName("app_role")
public class AppRole {

    /** 主键 ID，自增 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 角色名称，如"管理员" */
    private String roleName;

    /** 角色标识键，如 ADMIN */
    private String roleKey;

    /** 排序序号 */
    private Integer sortOrder;

    /** 状态：1=启用，0=禁用 */
    private Integer status;

    /** 角色备注说明 */
    private String remark;

    /** 创建时间，插入时自动填充 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /** 更新时间，插入和更新时自动填充 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
