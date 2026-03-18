package com.product.app.mapper;

import com.product.app.entity.AppRole;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 角色数据访问接口，继承 MyBatis-Plus BaseMapper，提供角色表的基础 CRUD 操作。
 */
@Mapper
public interface AppRoleMapper extends BaseMapper<AppRole> {
}
