package com.auth.platform.mapper;

import com.auth.platform.entity.SysUserRole;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 用户-角色关联 Mapper 接口
 * <p>继承 MyBatis-Plus 的 {@link BaseMapper}，自动获得对 sys_user_role 中间表的基础 CRUD 操作。
 * 扩展了按用户 ID 批量删除关联记录的方法，用于用户角色的全量替换场景。</p>
 */
@Mapper
public interface SysUserRoleMapper extends BaseMapper<SysUserRole> {

    /**
     * 根据用户 ID 删除该用户的所有角色关联记录
     * <p>用于更新用户角色时，先清空旧关联再批量插入新关联（全量替换策略）。</p>
     *
     * @param userId 用户 ID
     * @return 删除的记录条数
     */
    @Delete("DELETE FROM sys_user_role WHERE user_id = #{userId}")
    int deleteByUserId(@Param("userId") Long userId);
}
