package com.product.app.mapper;

import com.product.app.entity.AppUserRole;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 用户角色关联数据访问接口，继承 MyBatis-Plus BaseMapper，
 * 额外提供按用户 ID 批量删除角色关联的方法。
 */
@Mapper
public interface AppUserRoleMapper extends BaseMapper<AppUserRole> {

    /**
     * 根据用户 ID 删除该用户的所有角色关联记录。
     *
     * @param userId 用户 ID
     * @return 删除的记录条数
     */
    @Delete("DELETE FROM app_user_role WHERE user_id = #{userId}")
    int deleteByUserId(@Param("userId") Long userId);
}
