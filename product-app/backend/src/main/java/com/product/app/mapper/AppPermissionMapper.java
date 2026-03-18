package com.product.app.mapper;

import com.product.app.entity.AppPermission;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 权限数据访问接口，继承 MyBatis-Plus BaseMapper，提供权限表的基础 CRUD 操作。
 */
@Mapper
public interface AppPermissionMapper extends BaseMapper<AppPermission> {
}
