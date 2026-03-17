package com.product.app.service.impl;

import com.product.app.common.BusinessException;
import com.product.app.common.PageQuery;
import com.product.app.dto.UserInfo;
import com.product.app.dto.UserUpdateRequest;
import com.product.app.entity.AppUser;
import com.product.app.entity.AppUserOauth;
import com.product.app.mapper.AppUserMapper;
import com.product.app.mapper.AppUserOauthMapper;
import com.product.app.service.AppUserService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AppUserServiceImpl extends ServiceImpl<AppUserMapper, AppUser> implements AppUserService {

    private final AppUserMapper userMapper;
    private final AppUserOauthMapper oauthMapper;

    @Override
    public UserInfo getUserInfo(Long userId) {
        AppUser user = userMapper.selectById(userId);
        if (user == null) throw new BusinessException(404, "用户不存在");

        UserInfo info = new UserInfo();
        info.setId(user.getId());
        info.setUsername(user.getUsername());
        info.setEmail(user.getEmail());
        info.setPhone(user.getPhone());
        info.setNickname(user.getNickname());
        info.setAvatar(user.getAvatar());
        info.setStatus(user.getStatus());
        info.setRoles(userMapper.selectRoleKeysByUserId(userId));
        info.setPermissions(userMapper.selectPermissionKeysByUserId(userId));

        // OAuth绑定信息
        List<AppUserOauth> oauths = oauthMapper.selectList(
                new LambdaQueryWrapper<AppUserOauth>().eq(AppUserOauth::getUserId, userId));
        info.setOauthBindings(oauths.stream().map(o -> {
            var b = new UserInfo.OAuthBinding();
            b.setProvider(o.getOauthProvider());
            b.setOauthUsername(o.getOauthUsername());
            return b;
        }).toList());

        return info;
    }

    @Override
    public IPage<UserInfo> pageUsers(PageQuery query) {
        Page<AppUser> page = new Page<>(query.getPageNum(), query.getPageSize());
        LambdaQueryWrapper<AppUser> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(query.getKeyword())) {
            wrapper.like(AppUser::getUsername, query.getKeyword())
                    .or().like(AppUser::getNickname, query.getKeyword());
        }
        wrapper.orderByDesc(AppUser::getCreatedAt);
        return userMapper.selectPage(page, wrapper).convert(u -> getUserInfo(u.getId()));
    }

    @Override
    public void updateProfile(Long userId, UserUpdateRequest request) {
        AppUser user = userMapper.selectById(userId);
        if (user == null) throw new BusinessException(404, "用户不存在");
        if (request.getEmail() != null) user.setEmail(request.getEmail());
        if (request.getPhone() != null) user.setPhone(request.getPhone());
        if (request.getNickname() != null) user.setNickname(request.getNickname());
        if (request.getAvatar() != null) user.setAvatar(request.getAvatar());
        userMapper.updateById(user);
    }

    @Override
    public void unbindOAuth(Long userId, String provider) {
        AppUser user = userMapper.selectById(userId);
        if (user == null) throw new BusinessException(404, "用户不存在");

        // 检查是否满足解绑条件
        String password = user.getPassword();
        boolean hasPassword = password != null && !password.isEmpty();
        long oauthCount = oauthMapper.selectCount(
                new LambdaQueryWrapper<AppUserOauth>().eq(AppUserOauth::getUserId, userId));
        if (!hasPassword && oauthCount == 1) {
            throw new BusinessException(400, "解绑失败：您没有设置密码，且这是您最后一个登录方式，解绑后将无法登录");
        }

        int deleted = oauthMapper.delete(
                new LambdaQueryWrapper<AppUserOauth>()
                        .eq(AppUserOauth::getUserId, userId)
                        .eq(AppUserOauth::getOauthProvider, provider));
        if (deleted == 0) throw new BusinessException(404, "未找到该绑定关系");
    }
}
