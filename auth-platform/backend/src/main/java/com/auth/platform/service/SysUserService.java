package com.auth.platform.service;

import com.auth.platform.common.PageQuery;
import com.auth.platform.dto.*;
import com.auth.platform.entity.SysUser;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 系统用户管理服务接口
 * <p>继承 MyBatis-Plus 的 {@link IService}，获得基础 CRUD 能力。
 * 扩展了用户分页查询、用户创建（含角色分配）、用户更新、删除和密码修改等业务功能。</p>
 */
public interface SysUserService extends IService<SysUser> {

    /**
     * 分页查询用户列表（含角色和权限信息）
     * <p>支持按用户名、昵称、邮箱关键字模糊搜索，结果包含每个用户的角色和权限列表。</p>
     *
     * @param query 分页查询参数（pageNum、pageSize、keyword）
     * @return 分页用户信息列表，每条记录为 {@link UserInfo} DTO
     */
    IPage<UserInfo> pageUsers(PageQuery query);

    /**
     * 查询单个用户的详细信息（含角色和权限）
     *
     * @param userId 用户 ID
     * @return 用户信息 DTO（含角色列表和权限列表）
     */
    UserInfo getUserInfo(Long userId);

    /**
     * 创建新用户（含密码加密和角色分配）
     * <p>事务操作，确保插入用户和分配角色的原子性。用户名重复时抛出业务异常。</p>
     *
     * @param request 用户创建请求（username、password、email、phone、nickname、roleIds）
     */
    void createUser(UserCreateRequest request);

    /**
     * 更新用户信息（含角色全量替换）
     * <p>非 null 字段才更新，roleIds 不为 null 时全量替换用户角色关联。</p>
     *
     * @param userId  要更新的用户 ID
     * @param request 更新请求（所有字段均为可选）
     */
    void updateUser(Long userId, UserUpdateRequest request);

    /**
     * 删除用户
     * <p>禁止删除 id=1 的超级管理员账号，防止系统失去管理能力。</p>
     *
     * @param userId 要删除的用户 ID
     */
    void deleteUser(Long userId);

    /**
     * 修改用户密码（需要验证旧密码）
     * <p>使用 BCrypt 验证旧密码正确性，通过后将新密码加密存储。</p>
     *
     * @param userId  要修改密码的用户 ID
     * @param request 包含旧密码和新密码的请求对象
     */
    void changePassword(Long userId, PasswordChangeRequest request);
}
