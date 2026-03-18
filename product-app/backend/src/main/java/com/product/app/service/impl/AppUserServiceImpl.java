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

/**
 * 用户服务实现类，提供用户信息查询、资料修改、密码设置及 OAuth2 解绑等业务逻辑。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AppUserServiceImpl extends ServiceImpl<AppUserMapper, AppUser> implements AppUserService {

    private final AppUserMapper userMapper;
    private final AppUserOauthMapper oauthMapper;
    private final OAuth2Properties oauth2Props;
    private final PasswordEncoder passwordEncoder;

    /**
     * 根据用户 ID 查询用户详细信息，包含角色列表、权限列表、是否设置密码及 OAuth2 绑定情况。
     *
     * @param userId 用户 ID
     * @return 用户信息 DTO
     */
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

        // 判断用户是否已设置密码
        info.setHasPassword(StringUtils.hasText(user.getPassword()));

        // 查询并组装 OAuth2 绑定信息
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

    /**
     * 分页查询用户列表，支持按用户名或昵称关键词模糊搜索，按创建时间倒序排列。
     * 每条用户记录会进一步查询完整信息（含角色、权限等）。
     *
     * @param query 分页查询参数
     * @return 用户信息分页数据
     */
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

    /**
     * 修改指定用户的个人资料，仅更新请求中非 null 的字段。
     * 用户不存在时抛出 404 业务异常。
     *
     * @param userId  用户 ID
     * @param request 用户资料更新请求
     */
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

    /**
     * 解绑指定用户的 OAuth2 账号，同时向授权服务器发起撤销授权同意请求。
     * 若用户未设置密码且该 OAuth2 账号是最后一个登录方式，则禁止解绑。
     *
     * @param userId   用户 ID
     * @param provider OAuth2 提供商标识
     */
    @Override
    public void unbindOAuth(Long userId, String provider) {
        AppUser user = userMapper.selectById(userId);
        if (user == null) throw new BusinessException(404, "用户不存在");

        // 查询绑定记录，需获取 oauthUsername 以便撤销授权同意
        AppUserOauth record = oauthMapper.selectOne(
                new LambdaQueryWrapper<AppUserOauth>()
                        .eq(AppUserOauth::getUserId, userId)
                        .eq(AppUserOauth::getOauthProvider, provider));
        if (record == null) throw new BusinessException(404, "未找到该绑定关系");

        // 校验解绑安全条件：未设置密码且仅剩一个 OAuth2 登录方式时禁止解绑
        boolean hasPassword = StringUtils.hasText(user.getPassword());
        long oauthCount = oauthMapper.selectCount(
                new LambdaQueryWrapper<AppUserOauth>().eq(AppUserOauth::getUserId, userId));
        if (!hasPassword && oauthCount == 1) {
            throw new BusinessException(400, "解绑失败：您没有设置密码，且这是您最后一个登录方式，解绑后将无法登录");
        }

        oauthMapper.deleteById(record.getId());

        // 撤销授权服务器上的授权同意，使下次重新绑定时重新展示授权确认页
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

    /**
     * 设置或修改用户密码。
     * 首次设置时无需旧密码；已有密码的用户修改时必须先验证旧密码。
     *
     * @param userId  用户 ID
     * @param request 密码设置请求，包含旧密码（可选）和新密码
     */
    @Override
    public void setPassword(Long userId, PasswordRequest request) {
        AppUser user = userMapper.selectById(userId);
        if (user == null) throw new BusinessException(404, "用户不存在");

        boolean hasPassword = StringUtils.hasText(user.getPassword());
        if (hasPassword) {
            // 已有密码时需要验证旧密码
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
