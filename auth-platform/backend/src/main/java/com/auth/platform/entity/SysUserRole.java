package com.auth.platform.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 用户-角色关联实体类
 * <p>对应数据库表 sys_user_role，是用户与角色之间的多对多关联中间表。
 * 通过该表建立用户所拥有的角色列表，进而确定用户的权限范围。</p>
 */
@Data
@TableName("sys_user_role")
public class SysUserRole {
    /** 用户 ID，关联 sys_user.id */
    private Long userId;
    /** 角色 ID，关联 sys_role.id */
    private Long roleId;
}
