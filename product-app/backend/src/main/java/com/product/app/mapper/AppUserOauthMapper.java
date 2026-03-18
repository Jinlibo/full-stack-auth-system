package com.product.app.mapper;

import com.product.app.entity.AppUserOauth;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户 OAuth2 绑定数据访问接口，继承 MyBatis-Plus BaseMapper，
 * 提供用户 OAuth2 绑定表的基础 CRUD 操作。
 */
@Mapper
public interface AppUserOauthMapper extends BaseMapper<AppUserOauth> {
}
