package com.product.app.service;

import com.product.app.common.PageQuery;
import com.product.app.dto.UserInfo;
import com.product.app.dto.UserUpdateRequest;
import com.product.app.entity.AppUser;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

public interface AppUserService extends IService<AppUser> {
    UserInfo getUserInfo(Long userId);

    IPage<UserInfo> pageUsers(PageQuery query);

    void updateProfile(Long userId, UserUpdateRequest request);

    void unbindOAuth(Long userId, String provider);
}
