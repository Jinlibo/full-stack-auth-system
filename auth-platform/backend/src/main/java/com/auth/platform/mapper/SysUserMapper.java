package com.auth.platform.mapper;

import com.auth.platform.entity.SysUser;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 系统用户 Mapper 接口
 * <p>继承 MyBatis-Plus 的 {@link BaseMapper}，自动获得对 sys_user 表的基础 CRUD 操作。
 * 扩展了两个自定义联表查询方法，用于加载用户的角色和权限列表。</p>
 */
@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {

    /**
     * 根据用户 ID 查询该用户拥有的所有角色标识列表
     * <p>联表查询 sys_user_role 和 sys_role，仅返回状态正常（status=1）的角色。</p>
     *
     * @param userId 用户 ID
     * @return 角色标识列表，如 ["SUPER_ADMIN", "USER"]
     */
    @Select("SELECT r.role_key FROM sys_role r " +
            "INNER JOIN sys_user_role ur ON r.id = ur.role_id " +
            "WHERE ur.user_id = #{userId} AND r.status = 1")
    List<String> selectRoleKeysByUserId(@Param("userId") Long userId);

    /**
     * 根据用户 ID 查询该用户（通过其角色）拥有的所有权限标识列表
     * <p>三表联查 sys_user_role、sys_role_permission 和 sys_permission，
     * 使用 DISTINCT 去重，仅返回状态正常（status=1）的权限。</p>
     *
     * @param userId 用户 ID
     * @return 权限标识列表，如 ["system:user:add", "product:list"]
     */
    @Select("SELECT DISTINCT p.permission_key FROM sys_permission p " +
            "INNER JOIN sys_role_permission rp ON p.id = rp.permission_id " +
            "INNER JOIN sys_user_role ur ON rp.role_id = ur.role_id " +
            "WHERE ur.user_id = #{userId} AND p.status = 1")
    List<String> selectPermissionKeysByUserId(@Param("userId") Long userId);
}
