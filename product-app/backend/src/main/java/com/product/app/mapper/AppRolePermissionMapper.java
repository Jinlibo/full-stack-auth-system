package com.product.app.mapper;

import com.product.app.entity.AppRolePermission;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AppRolePermissionMapper extends BaseMapper<AppRolePermission> {
    @Delete("DELETE FROM app_role_permission WHERE role_id = #{roleId}")
    int deleteByRoleId(@Param("roleId") Long roleId);
}
