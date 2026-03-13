package com.product.app.mapper;

import com.product.app.entity.AppUser;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface AppUserMapper extends BaseMapper<AppUser> {
    @Select("SELECT r.role_key FROM app_role r INNER JOIN app_user_role ur ON r.id = ur.role_id WHERE ur.user_id = #{userId} AND r.status = 1")
    List<String> selectRoleKeysByUserId(@Param("userId") Long userId);

    @Select("SELECT DISTINCT p.permission_key FROM app_permission p INNER JOIN app_role_permission rp ON p.id = rp.permission_id INNER JOIN app_user_role ur ON rp.role_id = ur.role_id WHERE ur.user_id = #{userId} AND p.status = 1")
    List<String> selectPermissionKeysByUserId(@Param("userId") Long userId);
}
