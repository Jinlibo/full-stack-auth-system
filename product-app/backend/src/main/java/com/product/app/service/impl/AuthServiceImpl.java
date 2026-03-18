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
import com.product.app.service.AppUserService;
import com.product.app.service.AuthService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 认证服务实现类，提供账号密码登录、用户注册、OAuth2 完整登录流程（首次登录/绑定已有账号）、
 * OAuth2 账号绑定及退出登录等核心认证业务逻辑。
 */
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
    private final PasswordEncoder passwordEncoder;
    private final AppUserService appUserService;

    /** Redis 中临时存储 OAuth2 待绑定信息的键前缀 */
    private static final String OAUTH_PENDING_PREFIX = "oauth:pending:";

    /** Redis 中临时存储 OAuth2 state 参数的键前缀（防 CSRF） */
    private static final String OAUTH_STATE_PREFIX = "oauth:state:";

    /**
     * 账号密码登录，通过 Spring Security AuthenticationManager 验证凭证，
     * 验证通过后颁发 JWT 令牌并存入 Redis。
     *
     * @param request 登录请求，包含用户名和密码
     * @return 登录响应，包含 accessToken 和用户信息
     */
    @Override
    public LoginResponse login(LoginRequest request) {
        Authentication auth = authManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));
        LoginUser loginUser = (LoginUser) auth.getPrincipal();
        return buildLoginResponse(loginUser.getUser(), loginUser);
    }

    /**
     * 用户注册，检查用户名唯一性后创建账号并分配默认普通用户角色（id=2）。
     *
     * @param request 注册请求，包含用户名、密码、邮箱等信息
     */
    @Override
    @Transactional
    public void register(RegisterRequest request) {
        // 检查用户名是否已存在
        Long count = userMapper.selectCount(
                new LambdaQueryWrapper<AppUser>().eq(AppUser::getUsername, request.getUsername()));
        if (count > 0) {
            throw new BusinessException(400, "用户名已存在");
        }

        // 创建用户记录
        AppUser user = new AppUser();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEmail(request.getEmail());
        user.setNickname(request.getNickname() != null ? request.getNickname() : request.getUsername());
        user.setPhone(request.getPhone());
        user.setStatus(1);
        userMapper.insert(user);

        // 分配默认角色（普通用户，id=2）
        AppUserRole ur = new AppUserRole();
        ur.setUserId(user.getId());
        ur.setRoleId(2L);
        userRoleMapper.insert(ur);
    }

    /**
     * OAuth2 登录回调处理，完整流程包含：
     * 1. 验证并消费 state 参数（防 CSRF）
     * 2. 用授权码换取 OAuth2 访问令牌
     * 3. 用访问令牌获取授权服务器的用户信息
     * 4. 查找本地已绑定账号并直接登录；首次登录则将信息存入 Redis 等待前端选择绑定方式
     *
     * @param request OAuth2 回调请求，包含授权码和 state
     * @return 已绑定用户的登录响应，或含临时令牌的待绑定响应
     */
    @Override
    @Transactional
    public LoginResponse oauthLogin(OAuth2CallbackRequest request) {
        validateAndConsumeState(request.getState());

        // 第一步：用授权码换取 OAuth2 访问令牌并获取用户信息
        log.info("OAuth2 token exchange: code={}", request.getCode());
        JSONObject userJson = fetchOAuthUserInfo(request.getCode());
        String oauthUid = userJson.getStr("sub");
        String oauthUsername = userJson.getStr("username");
        String nickname = userJson.getStr("nickname");
        String email = userJson.getStr("email");
        String avatar = userJson.getStr("avatar");
        String phone = userJson.getStr("phone");

        // 第三步：查找本地已绑定账号
        String provider = "auth-platform";
        AppUserOauth oauthBinding = oauthMapper.selectOne(
                new LambdaQueryWrapper<AppUserOauth>()
                        .eq(AppUserOauth::getOauthProvider, provider)
                        .eq(AppUserOauth::getOauthUid, oauthUid));

        if (oauthBinding != null) {
            // 已绑定：更新用户 OAuth 信息后直接生成本地 JWT（不持久化 OAuth 令牌到数据库）
            AppUser localUser = userMapper.selectById(oauthBinding.getUserId());
            oauthBinding.setOauthUsername(oauthUsername);
            oauthBinding.setOauthAvatar(avatar);
            oauthMapper.updateById(oauthBinding);

            // 第四步：生成本地 JWT 令牌
            var roles = userMapper.selectRoleKeysByUserId(localUser.getId());
            var perms = userMapper.selectPermissionKeysByUserId(localUser.getId());
            LoginUser loginUser = new LoginUser(localUser, roles, perms);
            return buildLoginResponse(localUser, loginUser);
        } else {
            // 首次登录，未绑定：将 OAuth2 信息存入 Redis（10分钟有效），让前端选择创建新账号或绑定已有账号
            String pendingToken = UUID.randomUUID().toString();
            JSONObject pendingData = new JSONObject();
            pendingData.put("oauthUid", oauthUid);
            pendingData.put("oauthUsername", oauthUsername);
            pendingData.put("nickname", nickname);
            pendingData.put("email", email);
            pendingData.put("avatar", avatar);
            pendingData.put("phone", phone);
            pendingData.put("provider", provider);
            redisTemplate.opsForValue().set(
                    OAUTH_PENDING_PREFIX + pendingToken,
                    pendingData.toString(),
                    10, TimeUnit.MINUTES);

            return LoginResponse.builder()
                    .pendingBind(true)
                    .oauthPendingToken(pendingToken)
                    .build();
        }
    }

    /**
     * OAuth2 首次登录后创建新本地账号，从 Redis 读取临时 OAuth2 信息，
     * 自动创建本地用户（用户名冲突时追加 oauthUid 后缀），分配默认角色并建立 OAuth2 绑定关系。
     *
     * @param oauthPendingToken Redis 中存储的临时授权令牌
     * @return 登录响应，包含新账号的 JWT 和用户信息
     */
    @Override
    @Transactional
    public LoginResponse oauthCreateNew(String oauthPendingToken) {
        String redisKey = OAUTH_PENDING_PREFIX + oauthPendingToken;
        String json = redisTemplate.opsForValue().get(redisKey);
        if (json == null) {
            throw new BusinessException(400, "临时授权已过期，请重新发起 OAuth 登录");
        }

        JSONObject data = JSONUtil.parseObj(json);
        String oauthUid = data.getStr("oauthUid");
        String oauthUsername = data.getStr("oauthUsername");
        String nickname = data.getStr("nickname");
        String email = data.getStr("email");
        String avatar = data.getStr("avatar");
        String phone = data.getStr("phone");
        String provider = data.getStr("provider");

        // 创建本地用户，若用户名已存在则追加 oauthUid 后缀避免冲突
        String desiredUsername = oauthUsername;
        Long conflict = userMapper.selectCount(
                new LambdaQueryWrapper<AppUser>().eq(AppUser::getUsername, desiredUsername));
        if (conflict > 0) {
            desiredUsername = oauthUsername + "_" + oauthUid;
        }
        AppUser localUser = new AppUser();
        localUser.setUsername(desiredUsername);
        localUser.setNickname(nickname != null ? nickname : oauthUsername);
        localUser.setEmail(email);
        localUser.setAvatar(avatar);
        localUser.setPhone(phone);
        localUser.setStatus(1);
        userMapper.insert(localUser);

        // 分配默认角色（普通用户，id=2）
        AppUserRole ur = new AppUserRole();
        ur.setUserId(localUser.getId());
        ur.setRoleId(2L);
        userRoleMapper.insert(ur);

        // 创建 OAuth2 绑定记录（不持久化 OAuth 令牌到数据库）
        AppUserOauth oauthBinding = new AppUserOauth();
        oauthBinding.setUserId(localUser.getId());
        oauthBinding.setOauthProvider(provider);
        oauthBinding.setOauthUid(oauthUid);
        oauthBinding.setOauthUsername(oauthUsername);
        oauthBinding.setOauthAvatar(avatar);
        oauthMapper.insert(oauthBinding);

        // 删除 Redis 中的临时令牌
        redisTemplate.delete(redisKey);

        // 生成本地 JWT 令牌
        var roles = userMapper.selectRoleKeysByUserId(localUser.getId());
        var perms = userMapper.selectPermissionKeysByUserId(localUser.getId());
        LoginUser loginUser = new LoginUser(localUser, roles, perms);
        return buildLoginResponse(localUser, loginUser);
    }

    /**
     * OAuth2 首次登录后绑定已有本地账号，验证账号凭证后建立 OAuth2 绑定关系。
     * 若该 OAuth2 账号已绑定到其他用户则拒绝操作。
     *
     * @param oauthPendingToken Redis 中存储的临时授权令牌
     * @param username          已有本地账号的用户名
     * @param password          已有本地账号的密码
     * @return 登录响应，包含已有账号的 JWT 和用户信息
     */
    @Override
    @Transactional
    public LoginResponse oauthBindExisting(String oauthPendingToken, String username, String password) {
        String redisKey = OAUTH_PENDING_PREFIX + oauthPendingToken;
        String json = redisTemplate.opsForValue().get(redisKey);
        if (json == null) {
            throw new BusinessException(400, "临时授权已过期，请重新发起 OAuth 登录");
        }

        JSONObject data = JSONUtil.parseObj(json);
        String oauthUid = data.getStr("oauthUid");
        String oauthUsername = data.getStr("oauthUsername");
        String avatar = data.getStr("avatar");
        String provider = data.getStr("provider");

        // 验证已有本地账号的用户名密码
        Authentication auth = authManager.authenticate(
                new UsernamePasswordAuthenticationToken(username, password));
        LoginUser loginUser = (LoginUser) auth.getPrincipal();
        AppUser localUser = loginUser.getUser();

        // 检查该 OAuth2 账号是否已绑定到其他本地用户
        AppUserOauth existing = oauthMapper.selectOne(
                new LambdaQueryWrapper<AppUserOauth>()
                        .eq(AppUserOauth::getOauthProvider, provider)
                        .eq(AppUserOauth::getOauthUid, oauthUid));
        if (existing != null && !existing.getUserId().equals(localUser.getId())) {
            throw new BusinessException(400, "该OAuth账号已绑定到其他用户");
        }

        if (existing == null) {
            // 创建新的 OAuth2 绑定关系（不持久化 OAuth 令牌到数据库）
            AppUserOauth oauthBinding = new AppUserOauth();
            oauthBinding.setUserId(localUser.getId());
            oauthBinding.setOauthProvider(provider);
            oauthBinding.setOauthUid(oauthUid);
            oauthBinding.setOauthUsername(oauthUsername);
            oauthBinding.setOauthAvatar(avatar);
            oauthMapper.insert(oauthBinding);
        }

        // 删除 Redis 中的临时令牌
        redisTemplate.delete(redisKey);

        // 生成本地 JWT 令牌
        return buildLoginResponse(localUser, loginUser);
    }

    /**
     * 生成 OAuth2 授权地址。在构建 URL 前，先预检授权服务器上的客户端是否已启用，
     * 预检通过后才将 state 参数存入 Redis（防止预检失败产生残留记录）。
     *
     * @param state 前端传入的随机 state 参数，用于防 CSRF
     * @return 完整的 OAuth2 授权地址
     */
    @Override
    public String getOAuthAuthorizeUrl(String state) {
        String baseUrl = oauth2Props.getAuthServer().getBaseUrl();
        String clientId = oauth2Props.getClient().getClientId();

        // 预检：验证 auth-platform 中客户端是否已启用，提前返回友好错误信息
        // 若直接重定向，被禁用的客户端会收到 Spring Auth Server 不友好的 400 响应
        try (HttpResponse infoResp = HttpRequest.get(baseUrl + "/api/oauth2/client-info?clientId=" + clientId)
                .timeout(5000)
                .execute()) {
            if (infoResp.getStatus() == 200) {
                JSONObject info = JSONUtil.parseObj(infoResp.body());
                if (!info.getBool("enabled", true)) {
                    throw new BusinessException(400, "该应用当前已被停用，无法使用OAuth登录");
                }
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.warn("无法检查 OAuth 客户端状态，继续处理: {}", e.getMessage());
        }

        // 预检通过后再将 state 存入 Redis（有效期 10 分钟），避免预检失败时产生残留 state
        if (state != null && !state.isBlank()) {
            redisTemplate.opsForValue().set(OAUTH_STATE_PREFIX + state, "1", 10, TimeUnit.MINUTES);
        }

        String authorizeUri = oauth2Props.getAuthServer().getAuthorizeUri();
        String redirectUri = URLEncoder.encode(oauth2Props.getClient().getRedirectUri(), StandardCharsets.UTF_8);
        String scope = URLEncoder.encode(oauth2Props.getClient().getScope().replace(",", " "), StandardCharsets.UTF_8);

        return String.format("%s%s?response_type=code&client_id=%s&redirect_uri=%s&scope=%s&state=%s",
                baseUrl, authorizeUri, clientId, redirectUri, scope, state != null ? state : "");
    }

    /**
     * 退出登录，从 Redis 中删除当前用户的 JWT 令牌，使其立即失效。
     *
     * @param token 请求头中的 Bearer Token 字符串
     */
    @Override
    public void logout(String token) {
        if (token != null && token.startsWith("Bearer ")) token = token.substring(7);
        if (jwtUtil.validateToken(token)) {
            Long userId = jwtUtil.getUserIdFromToken(token);
            redisTemplate.delete("token:access:" + userId);
        }
    }

    /**
     * 将 OAuth2 账号绑定到当前已登录用户，流程包含：
     * 1. 验证并消费 state 参数（防 CSRF）
     * 2. 用授权码换取 OAuth2 访问令牌
     * 3. 用访问令牌获取授权服务器的用户信息
     * 4. 检查 OAuth2 账号未被其他用户绑定，然后创建或更新绑定记录
     *
     * @param userId  当前登录用户 ID
     * @param request OAuth2 回调请求，包含授权码和 state
     * @return 绑定后的最新用户信息（含更新后的 oauthBindings）
     */
    @Override
    @Transactional
    public UserInfo bindOAuth(Long userId, OAuth2CallbackRequest request) {
        validateAndConsumeState(request.getState());

        // 第一步：用授权码换取 OAuth2 访问令牌并获取用户信息
        JSONObject userJson = fetchOAuthUserInfo(request.getCode());
        String oauthUid = userJson.getStr("sub");
        String oauthUsername = userJson.getStr("username");
        String avatar = userJson.getStr("avatar");

        String provider = "auth-platform";

        // 第三步：检查该 OAuth2 账号是否已绑定到其他本地用户
        AppUserOauth existing = oauthMapper.selectOne(
                new LambdaQueryWrapper<AppUserOauth>()
                        .eq(AppUserOauth::getOauthProvider, provider)
                        .eq(AppUserOauth::getOauthUid, oauthUid));
        if (existing != null && !existing.getUserId().equals(userId)) {
            throw new BusinessException(400, "该OAuth账号已绑定到其他用户");
        }

        if (existing != null) {
            // 已绑定到当前用户，更新 OAuth2 用户信息（不持久化 OAuth 令牌到数据库）
            existing.setOauthUsername(oauthUsername);
            existing.setOauthAvatar(avatar);
            oauthMapper.updateById(existing);
        } else {
            // 新建绑定关系（不持久化 OAuth 令牌到数据库）
            AppUserOauth binding = new AppUserOauth();
            binding.setUserId(userId);
            binding.setOauthProvider(provider);
            binding.setOauthUid(oauthUid);
            binding.setOauthUsername(oauthUsername);
            binding.setOauthAvatar(avatar);
            oauthMapper.insert(binding);
        }

        // 第四步：返回完整用户信息（包含更新后的 OAuth2 绑定列表）
        return appUserService.getUserInfo(userId);
    }

    /**
     * 验证并消费 OAuth2 state 参数，防止 CSRF 攻击。
     * state 不存在或已过期（Redis 中无对应记录）时抛出业务异常。
     *
     * @param state 前端传入的 state 参数
     */
    private void validateAndConsumeState(String state) {
        if (state == null || state.isBlank()) {
            throw new BusinessException(400, "缺少 state 参数，请重新发起 OAuth 登录");
        }
        if (!redisTemplate.delete(OAUTH_STATE_PREFIX + state)) {
            throw new BusinessException(400, "state 无效或已过期，请重新发起 OAuth 登录");
        }
    }

    /**
     * 使用 OAuth2 授权码换取访问令牌，再用令牌从授权服务器获取用户信息。
     *
     * @param code OAuth2 授权码
     * @return 授权服务器返回的用户信息 JSON 对象（含 sub、username、nickname、email、avatar 等字段）
     */
    private JSONObject fetchOAuthUserInfo(String code) {
        String tokenUrl = oauth2Props.getAuthServer().getBaseUrl() + oauth2Props.getAuthServer().getTokenUri();
        String accessToken;
        try (HttpResponse tokenResp = HttpRequest.post(tokenUrl)
                .form("grant_type", "authorization_code")
                .form("code", code)
                .form("redirect_uri", oauth2Props.getClient().getRedirectUri())
                .form("client_id", oauth2Props.getClient().getClientId())
                .form("client_secret", oauth2Props.getClient().getClientSecret())
                .timeout(10000)
                .execute()) {
            if (tokenResp.getStatus() != 200) {
                throw new BusinessException(400, "OAuth2授权失败: " + tokenResp.body());
            }
            accessToken = JSONUtil.parseObj(tokenResp.body()).getStr("access_token");
        }
        String userinfoUrl = oauth2Props.getAuthServer().getBaseUrl() + oauth2Props.getAuthServer().getUserinfoUri();
        try (HttpResponse userResp = HttpRequest.get(userinfoUrl)
                .header("Authorization", "Bearer " + accessToken)
                .timeout(10000)
                .execute()) {
            if (userResp.getStatus() != 200) {
                throw new BusinessException(400, "获取用户信息失败");
            }
            return JSONUtil.parseObj(userResp.body());
        }
    }

    /**
     * 构建登录响应，生成 JWT 访问令牌并存入 Redis（滑动窗口，每次有效请求刷新 TTL），
     * 然后组装包含 accessToken 和用户信息的登录响应。
     *
     * @param user      本地用户实体
     * @param loginUser 登录用户信息（含角色和权限）
     * @return 登录响应 DTO
     */
    private LoginResponse buildLoginResponse(AppUser user, LoginUser loginUser) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("roles", loginUser.getRoles());
        String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getUsername(), claims);

        // 将 JWT 存入 Redis，TTL 由配置决定（默认 2 小时），采用滑动窗口机制
        redisTemplate.opsForValue().set(
                "token:access:" + user.getId(),
                accessToken,
                jwtUtil.getExpiration(),
                TimeUnit.MILLISECONDS
        );

        return LoginResponse.builder()
                .accessToken(accessToken)
                .userInfo(appUserService.getUserInfo(user.getId()))
                .build();
    }
}
