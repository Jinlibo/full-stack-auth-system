package com.product.app.service.impl;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.product.app.common.BusinessException;
import com.product.app.config.OAuth2Properties;
import com.product.app.dto.*;
import com.product.app.entity.AppUser;
import com.product.app.entity.AppUserOauth;
import com.product.app.entity.AppUserRole;
import com.product.app.mapper.AppUserMapper;
import com.product.app.mapper.AppUserOauthMapper;
import com.product.app.mapper.AppUserRoleMapper;
import com.product.app.security.JwtUtil;
import com.product.app.security.LoginUser;
import com.product.app.service.AuthService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authManager;
    private final JwtUtil jwtUtil;
    private final AppUserMapper userMapper;
    private final AppUserOauthMapper oauthMapper;
    private final AppUserRoleMapper userRoleMapper;
    private final OAuth2Properties oauth2Props;
    private final StringRedisTemplate redisTemplate;

    @Override
    public LoginResponse login(LoginRequest request) {
        Authentication auth = authManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));
        LoginUser loginUser = (LoginUser) auth.getPrincipal();
        return buildLoginResponse(loginUser.getUser(), loginUser);
    }

    @Override
    @Transactional
    public LoginResponse oauthLogin(OAuth2CallbackRequest request) {
        // 1. 用授权码换取access_token
        String tokenUrl = oauth2Props.getAuthServer().getBaseUrl() + oauth2Props.getAuthServer().getTokenUri();
        log.info("OAuth2 token exchange: url={}, code={}", tokenUrl, request.getCode());

        HttpResponse tokenResp = HttpRequest.post(tokenUrl)
                .form("grant_type", "authorization_code")
                .form("code", request.getCode())
                .form("redirect_uri", oauth2Props.getClient().getRedirectUri())
                .form("client_id", oauth2Props.getClient().getClientId())
                .form("client_secret", oauth2Props.getClient().getClientSecret())
                .timeout(10000)
                .execute();

        if (tokenResp.getStatus() != 200) {
            log.error("OAuth2 token exchange failed: {}", tokenResp.body());
            throw new BusinessException(400, "OAuth2授权失败: " + tokenResp.body());
        }

        JSONObject tokenJson = JSONUtil.parseObj(tokenResp.body());
        String accessToken = tokenJson.getStr("access_token");
        String refreshToken = tokenJson.getStr("refresh_token");

        // 2. 用access_token获取用户信息
        String userinfoUrl = oauth2Props.getAuthServer().getBaseUrl() + oauth2Props.getAuthServer().getUserinfoUri();
        HttpResponse userResp = HttpRequest.get(userinfoUrl)
                .header("Authorization", "Bearer " + accessToken)
                .timeout(10000)
                .execute();

        if (userResp.getStatus() != 200) {
            log.error("OAuth2 userinfo failed: {}", userResp.body());
            throw new BusinessException(400, "获取用户信息失败");
        }

        JSONObject userJson = JSONUtil.parseObj(userResp.body());
        String oauthUid = userJson.getStr("sub");
        String oauthUsername = userJson.getStr("username");
        String nickname = userJson.getStr("nickname");
        String email = userJson.getStr("email");
        String avatar = userJson.getStr("avatar");

        // 3. 查找或创建本地用户
        String provider = "auth-platform";
        AppUserOauth oauthBinding = oauthMapper.selectOne(
                new LambdaQueryWrapper<AppUserOauth>()
                        .eq(AppUserOauth::getOauthProvider, provider)
                        .eq(AppUserOauth::getOauthUid, oauthUid));

        AppUser localUser;
        if (oauthBinding != null) {
            // 已绑定,更新token
            localUser = userMapper.selectById(oauthBinding.getUserId());
            oauthBinding.setAccessToken(accessToken);
            oauthBinding.setRefreshToken(refreshToken);
            oauthBinding.setOauthUsername(oauthUsername);
            oauthBinding.setOauthAvatar(avatar);
            oauthMapper.updateById(oauthBinding);
        } else {
            // 新用户: 创建本地用户 + 绑定
            // 优先使用授权服务器上的真实用户名；若与已有本地账号冲突则加后缀区分
            String desiredUsername = oauthUsername;
            Long conflict = userMapper.selectCount(
                    new LambdaQueryWrapper<AppUser>().eq(AppUser::getUsername, desiredUsername));
            if (conflict > 0) {
                desiredUsername = oauthUsername + "_" + oauthUid;
            }
            localUser = new AppUser();
            localUser.setUsername(desiredUsername);
            localUser.setNickname(nickname != null ? nickname : oauthUsername);
            localUser.setEmail(email);
            localUser.setAvatar(avatar);
            localUser.setStatus(1);
            userMapper.insert(localUser);

            // 分配默认角色(普通用户, id=2)
            AppUserRole ur = new AppUserRole();
            ur.setUserId(localUser.getId());
            ur.setRoleId(2L);
            userRoleMapper.insert(ur);

            // 创建绑定
            oauthBinding = new AppUserOauth();
            oauthBinding.setUserId(localUser.getId());
            oauthBinding.setOauthProvider(provider);
            oauthBinding.setOauthUid(oauthUid);
            oauthBinding.setOauthUsername(oauthUsername);
            oauthBinding.setOauthAvatar(avatar);
            oauthBinding.setAccessToken(accessToken);
            oauthBinding.setRefreshToken(refreshToken);
            oauthMapper.insert(oauthBinding);
        }

        // 4. 生成本地JWT
        var roles = userMapper.selectRoleKeysByUserId(localUser.getId());
        var perms = userMapper.selectPermissionKeysByUserId(localUser.getId());
        LoginUser loginUser = new LoginUser(localUser, roles, perms);
        return buildLoginResponse(localUser, loginUser);
    }

    @Override
    public String getOAuthAuthorizeUrl(String state) {
        String baseUrl = oauth2Props.getAuthServer().getBaseUrl();
        String clientId = oauth2Props.getClient().getClientId();

        // Pre-check: verify the client is enabled in the auth-platform before redirecting.
        // A disabled client causes a cryptic 400 from Spring Auth Server; we give a clear error here.
        try {
            HttpResponse infoResp = HttpRequest.get(baseUrl + "/api/oauth2/client-info?clientId=" + clientId)
                    .timeout(5000)
                    .execute();
            if (infoResp.getStatus() == 200) {
                JSONObject info = JSONUtil.parseObj(infoResp.body());
                if (!info.getBool("enabled", true)) {
                    throw new BusinessException(400, "该应用当前已被停用，无法使用OAuth登录");
                }
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Unable to check OAuth client status, proceeding: {}", e.getMessage());
        }

        String authorizeUri = oauth2Props.getAuthServer().getAuthorizeUri();
        String redirectUri = URLEncoder.encode(oauth2Props.getClient().getRedirectUri(), StandardCharsets.UTF_8);
        String scope = URLEncoder.encode(oauth2Props.getClient().getScope().replace(",", " "), StandardCharsets.UTF_8);

        return String.format("%s%s?response_type=code&client_id=%s&redirect_uri=%s&scope=%s&state=%s",
                baseUrl, authorizeUri, clientId, redirectUri, scope, state != null ? state : "");
    }

    @Override
    public void logout(String token) {
        if (token != null && token.startsWith("Bearer ")) token = token.substring(7);
        if (jwtUtil.validateToken(token)) {
            redisTemplate.opsForValue().set("token:blacklist:" + token, "1",
                    jwtUtil.getExpiration(), TimeUnit.MILLISECONDS);
        }
    }

    @Override
    @Transactional
    public UserInfo bindOAuth(Long userId, OAuth2CallbackRequest request) {
        // 1. Exchange code for access_token
        String tokenUrl = oauth2Props.getAuthServer().getBaseUrl() + oauth2Props.getAuthServer().getTokenUri();
        HttpResponse tokenResp = HttpRequest.post(tokenUrl)
                .form("grant_type", "authorization_code")
                .form("code", request.getCode())
                .form("redirect_uri", oauth2Props.getClient().getRedirectUri())
                .form("client_id", oauth2Props.getClient().getClientId())
                .form("client_secret", oauth2Props.getClient().getClientSecret())
                .timeout(10000)
                .execute();
        if (tokenResp.getStatus() != 200) {
            throw new BusinessException(400, "OAuth2授权失败: " + tokenResp.body());
        }
        JSONObject tokenJson = JSONUtil.parseObj(tokenResp.body());
        String accessToken = tokenJson.getStr("access_token");
        String refreshToken = tokenJson.getStr("refresh_token");

        // 2. Fetch user info from auth-platform
        String userinfoUrl = oauth2Props.getAuthServer().getBaseUrl() + oauth2Props.getAuthServer().getUserinfoUri();
        HttpResponse userResp = HttpRequest.get(userinfoUrl)
                .header("Authorization", "Bearer " + accessToken)
                .timeout(10000)
                .execute();
        if (userResp.getStatus() != 200) {
            throw new BusinessException(400, "获取用户信息失败");
        }
        JSONObject userJson = JSONUtil.parseObj(userResp.body());
        String oauthUid = userJson.getStr("sub");
        String oauthUsername = userJson.getStr("username");
        String avatar = userJson.getStr("avatar");

        String provider = "auth-platform";

        // 3. Ensure this OAuth account is not already bound to a different local user
        AppUserOauth existing = oauthMapper.selectOne(
                new LambdaQueryWrapper<AppUserOauth>()
                        .eq(AppUserOauth::getOauthProvider, provider)
                        .eq(AppUserOauth::getOauthUid, oauthUid));
        if (existing != null && !existing.getUserId().equals(userId)) {
            throw new BusinessException(400, "该OAuth账号已绑定到其他用户");
        }

        if (existing != null) {
            // Already bound to this user — just refresh tokens
            existing.setAccessToken(accessToken);
            existing.setRefreshToken(refreshToken);
            existing.setOauthUsername(oauthUsername);
            existing.setOauthAvatar(avatar);
            oauthMapper.updateById(existing);
        } else {
            // New binding
            AppUserOauth binding = new AppUserOauth();
            binding.setUserId(userId);
            binding.setOauthProvider(provider);
            binding.setOauthUid(oauthUid);
            binding.setOauthUsername(oauthUsername);
            binding.setOauthAvatar(avatar);
            binding.setAccessToken(accessToken);
            binding.setRefreshToken(refreshToken);
            oauthMapper.insert(binding);
        }

        // 4. Return complete user info (including updated oauthBindings)
        AppUser user = userMapper.selectById(userId);
        var roles = userMapper.selectRoleKeysByUserId(userId);
        var perms = userMapper.selectPermissionKeysByUserId(userId);
        return buildUserInfo(user, roles, perms);
    }

    private LoginResponse buildLoginResponse(AppUser user, LoginUser loginUser) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("roles", loginUser.getRoles());
        String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getUsername(), claims);
        String refreshToken = jwtUtil.generateRefreshToken(user.getId());

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(jwtUtil.getExpiration() / 1000)
                .userInfo(buildUserInfo(user, loginUser.getRoles(), loginUser.getPermissions()))
                .build();
    }

    private UserInfo buildUserInfo(AppUser user, java.util.List<String> roles, java.util.List<String> perms) {
        UserInfo info = new UserInfo();
        info.setId(user.getId());
        info.setUsername(user.getUsername());
        info.setEmail(user.getEmail());
        info.setPhone(user.getPhone());
        info.setNickname(user.getNickname());
        info.setAvatar(user.getAvatar());
        info.setStatus(user.getStatus());
        info.setRoles(roles);
        info.setPermissions(perms);
        // Always include OAuth bindings so login response is complete
        java.util.List<com.product.app.entity.AppUserOauth> oauths = oauthMapper.selectList(
                new LambdaQueryWrapper<AppUserOauth>().eq(AppUserOauth::getUserId, user.getId()));
        info.setOauthBindings(oauths.stream().map(o -> {
            var b = new UserInfo.OAuthBinding();
            b.setProvider(o.getOauthProvider());
            b.setOauthUsername(o.getOauthUsername());
            return b;
        }).toList());
        return info;
    }
}
