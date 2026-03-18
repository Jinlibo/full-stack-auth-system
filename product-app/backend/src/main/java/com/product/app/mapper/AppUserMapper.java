package com.product.app.mapper;

import com.product.app.entity.AppUser;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 用户数据访问接口，继承 MyBatis-Plus BaseMapper，
 * 额外提供查询用户角色标识和权限标识的自定义 SQL 方法。
 */
@Mapper
public interface AppUserMapper extends BaseMapper<AppUser> {

    /**
     * 查询指定用户拥有的所有启用角色的标识键列表。
     *
     * @param userId 用户 ID
     * @return 角色标识键列表，如 ["ADMIN", "USER"]
     */
    @Select("SELECT r.role_key FROM app_role r INNER JOIN app_user_role ur ON r.id = ur.role_id WHERE ur.user_id = #{userId} AND r.status = 1")
    List<String> selectRoleKeysByUserId(@Param("userId") Long userId);

    /**
     * 查询指定用户通过角色间接拥有的所有启用权限标识键列表（去重）。
     *
     * @param userId 用户 ID
     * @return 权限标识键列表，如 ["user:list", "role:manage"]
     */
    @Select("SELECT DISTINCT p.permission_key FROM app_permission p INNER JOIN app_role_permission rp ON p.id = rp.permission_id INNER JOIN app_user_role ur ON rp.role_id = ur.role_id WHERE ur.user_id = #{userId} AND p.status = 1")
    List<String> selectPermissionKeysByUserId(@Param("userId") Long userId);
}
