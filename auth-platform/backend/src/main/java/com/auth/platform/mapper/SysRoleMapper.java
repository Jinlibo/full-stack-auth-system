package com.auth.platform.mapper;

import com.auth.platform.entity.SysRole;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 系统角色 Mapper 接口
 * <p>继承 MyBatis-Plus 的 {@link BaseMapper}，自动获得对 sys_role 表的基础 CRUD 操作。
 * 当前无自定义 SQL 方法，业务层通过 LambdaQueryWrapper 构造查询条件。</p>
 */
@Mapper
public interface SysRoleMapper extends BaseMapper<SysRole> {
}
