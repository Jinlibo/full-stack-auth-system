package com.product.app.mapper;

import com.product.app.entity.AppRolePermission;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 角色权限关联数据访问接口，继承 MyBatis-Plus BaseMapper，
 * 额外提供按角色 ID 批量删除权限关联的方法。
 */
@Mapper
public interface AppRolePermissionMapper extends BaseMapper<AppRolePermission> {

    /**
     * 根据角色 ID 删除该角色的所有权限关联记录。
     *
     * @param roleId 角色 ID
     * @return 删除的记录条数
     */
    @Delete("DELETE FROM app_role_permission WHERE role_id = #{roleId}")
    int deleteByRoleId(@Param("roleId") Long roleId);
}
