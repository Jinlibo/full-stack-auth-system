package com.product.app.service;

import com.product.app.common.PageQuery;
import com.product.app.dto.PasswordRequest;
import com.product.app.dto.UserInfo;
import com.product.app.dto.UserUpdateRequest;
import com.product.app.entity.AppUser;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 用户服务接口，定义用户信息查询、资料修改、密码设置及 OAuth2 解绑等操作。
 * 继承 MyBatis-Plus IService，提供通用 CRUD 能力。
 */
public interface AppUserService extends IService<AppUser> {

    /**
     * 根据用户 ID 查询用户详细信息，包含角色、权限及 OAuth2 绑定信息。
     *
     * @param userId 用户 ID
     * @return 用户信息 DTO
     */
    UserInfo getUserInfo(Long userId);

    /**
     * 分页查询用户列表，支持按用户名或昵称关键词搜索。
     *
     * @param query 分页查询参数
     * @return 用户信息分页数据
     */
    IPage<UserInfo> pageUsers(PageQuery query);

    /**
     * 修改指定用户的个人资料，仅更新请求中非 null 的字段。
     *
     * @param userId  用户 ID
     * @param request 用户资料更新请求
     */
    void updateProfile(Long userId, UserUpdateRequest request);

    /**
     * 解绑指定用户的某个 OAuth2 提供商账号，并撤销授权服务器上的授权同意。
     *
     * @param userId   用户 ID
     * @param provider OAuth2 提供商标识
     */
    void unbindOAuth(Long userId, String provider);

    /**
     * 设置或修改用户密码。首次设置无需旧密码；已有密码需先验证旧密码。
     *
     * @param userId  用户 ID
     * @param request 密码设置请求
     */
    void setPassword(Long userId, PasswordRequest request);
}
