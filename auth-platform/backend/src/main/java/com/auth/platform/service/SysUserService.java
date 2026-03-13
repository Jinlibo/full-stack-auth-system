package com.auth.platform.service;

import com.auth.platform.common.PageQuery;
import com.auth.platform.dto.*;
import com.auth.platform.entity.SysUser;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

public interface SysUserService extends IService<SysUser> {
    IPage<UserInfo> pageUsers(PageQuery query);

    UserInfo getUserInfo(Long userId);

    void createUser(UserCreateRequest request);

    void updateUser(Long userId, UserUpdateRequest request);

    void deleteUser(Long userId);

    void changePassword(Long userId, PasswordChangeRequest request);
}
