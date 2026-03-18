package com.auth.platform.mapper;

import com.auth.platform.entity.SysRolePermission;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 角色-权限关联 Mapper 接口
 * <p>继承 MyBatis-Plus 的 {@link BaseMapper}，自动获得对 sys_role_permission 中间表的基础 CRUD 操作。
 * 扩展了按角色 ID 批量删除关联记录的方法，用于角色权限的全量替换场景。</p>
 */
@Mapper
public interface SysRolePermissionMapper extends BaseMapper<SysRolePermission> {

    /**
     * 根据角色 ID 删除该角色的所有权限关联记录
     * <p>用于更新角色权限时，先清空旧关联再批量插入新关联（全量替换策略）。</p>
     *
     * @param roleId 角色 ID
     * @return 删除的记录条数
     */
    @Delete("DELETE FROM sys_role_permission WHERE role_id = #{roleId}")
    int deleteByRoleId(@Param("roleId") Long roleId);
}
