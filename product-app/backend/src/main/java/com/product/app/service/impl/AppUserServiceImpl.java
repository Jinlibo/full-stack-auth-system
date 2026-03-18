package com.product.app.service.impl;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.product.app.common.BusinessException;
import com.product.app.common.PageQuery;
import com.product.app.config.OAuth2Properties;
import com.product.app.dto.PasswordRequest;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AppUserServiceImpl extends ServiceImpl<AppUserMapper, AppUser> implements AppUserService {

    private final AppUserMapper userMapper;
    private final AppUserOauthMapper oauthMapper;
    private final OAuth2Properties oauth2Props;
    private final PasswordEncoder passwordEncoder;

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

        // 是否设置了密码
        info.setHasPassword(StringUtils.hasText(user.getPassword()));

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

        // 查出绑定记录（需获取 oauthUsername 用于撤销同意）
        AppUserOauth record = oauthMapper.selectOne(
                new LambdaQueryWrapper<AppUserOauth>()
                        .eq(AppUserOauth::getUserId, userId)
                        .eq(AppUserOauth::getOauthProvider, provider));
        if (record == null) throw new BusinessException(404, "未找到该绑定关系");

        // 检查是否满足解绑条件
        boolean hasPassword = StringUtils.hasText(user.getPassword());
        long oauthCount = oauthMapper.selectCount(
                new LambdaQueryWrapper<AppUserOauth>().eq(AppUserOauth::getUserId, userId));
        if (!hasPassword && oauthCount == 1) {
            throw new BusinessException(400, "解绑失败：您没有设置密码，且这是您最后一个登录方式，解绑后将无法登录");
        }

        oauthMapper.deleteById(record.getId());

        // 撤销 auth-platform 上的授权同意，使下次绑定时重新展示授权页
        try {
            String revokeUrl = oauth2Props.getAuthServer().getBaseUrl() + "/api/oauth2/revoke-consent";
            String body = String.format("{\"clientId\":\"%s\",\"clientSecret\":\"%s\",\"username\":\"%s\"}",
                    oauth2Props.getClient().getClientId(),
                    oauth2Props.getClient().getClientSecret(),
                    record.getOauthUsername());
            HttpResponse resp = HttpRequest.delete(revokeUrl)
                    .body(body, "application/json")
                    .timeout(5000)
                    .execute();
            if (resp.getStatus() != 200) {
                log.warn("撤销 OAuth 同意失败，状态码: {}, 响应: {}", resp.getStatus(), resp.body());
            }
        } catch (Exception e) {
            log.warn("撤销 OAuth 同意时发生异常，不影响解绑主流程: {}", e.getMessage());
        }
    }

    @Override
    public void setPassword(Long userId, PasswordRequest request) {
        AppUser user = userMapper.selectById(userId);
        if (user == null) throw new BusinessException(404, "用户不存在");

        boolean hasPassword = StringUtils.hasText(user.getPassword());
        if (hasPassword) {
            // 已有密码：需要验证旧密码
            if (!StringUtils.hasText(request.getOldPassword())) {
                throw new BusinessException(400, "请输入旧密码");
            }
            if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
                throw new BusinessException(400, "旧密码错误");
            }
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userMapper.updateById(user);
    }
}
