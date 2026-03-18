package com.auth.platform.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 角色-权限关联实体类
 * <p>对应数据库表 sys_role_permission，是角色与权限之间的多对多关联中间表。
 * 通过该表建立角色所拥有的权限节点列表。</p>
 */
@Data
@TableName("sys_role_permission")
public class SysRolePermission {
    /** 角色 ID，关联 sys_role.id */
    private Long roleId;
    /** 权限节点 ID，关联 sys_permission.id */
    private Long permissionId;
}
