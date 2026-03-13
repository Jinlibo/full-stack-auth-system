package com.product.app.mapper;

import com.product.app.entity.AppUserRole;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AppUserRoleMapper extends BaseMapper<AppUserRole> {
    @Delete("DELETE FROM app_user_role WHERE user_id = #{userId}")
    int deleteByUserId(@Param("userId") Long userId);
}
