package com.auth.platform.service.impl;

import com.auth.platform.common.BusinessException;
import com.auth.platform.dto.*;
import com.auth.platform.entity.SysUser;
import com.auth.platform.entity.SysUserRole;
import com.auth.platform.mapper.SysUserMapper;
import com.auth.platform.mapper.SysUserRoleMapper;
import com.auth.platform.security.JwtProperties;
import com.auth.platform.security.JwtUtil;
import com.auth.platform.security.LoginUser;
import com.auth.platform.service.AuthService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 认证服务实现类（AuthService Implementation）
 *
 * <p>实现了完整的 JWT 认证流程，包括：
 * <ul>
 *   <li>登录：Spring Security 认证 + JWT 生成 + Redis 缓存</li>
 *   <li>注册：用户创建 + 密码加密 + 默认角色分配</li>
 *   <li>刷新 Token：Refresh Token 验证 + 新 Token 对生成</li>
 *   <li>退出登录：Token 黑名单 + Redis 缓存清理</li>
 * </ul>
 *
 * <p>依赖关系：
 * <ul>
 *   <li>{@link AuthenticationManager}：Spring Security 的认证管理器，负责验证用户名/密码</li>
 *   <li>{@link JwtUtil}：JWT Token 的生成、解析和验证工具</li>
 *   <li>{@link StringRedisTemplate}：Redis 操作，用于 Token 缓存和黑名单管理</li>
 *   <li>{@link PasswordEncoder}：BCrypt 密码加密器</li>
 * </ul>
 *
 * @author auth-platform
 */
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    /**
     * Spring Security 认证管理器
     * 调用 authenticate() 时，内部会：
     * 1. 调用 UserDetailsServiceImpl.loadUserByUsername() 加载用户
     * 2. 使用 BCryptPasswordEncoder.matches() 比对密码
     * 3. 校验账号状态（是否被禁用、锁定等）
     * 4. 成功则返回 Authentication 对象；失败则抛出 AuthenticationException
     */
    private final AuthenticationManager authenticationManager;

    /**
     * JWT 工具类，负责 Token 的生成、解析和验证
     */
    private final JwtUtil jwtUtil;

    /**
     * JWT 配置属性（expiration、refreshExpiration、secret、issuer）
     * 通过 @ConfigurationProperties(prefix = "jwt") 从 application.yml 读取
     */
    private final JwtProperties jwtProperties;

    /** 用户 Mapper，用于查询和插入用户数据 */
    private final SysUserMapper userMapper;

    /** 用户角色关联 Mapper，用于管理 sys_user_role 表 */
    private final SysUserRoleMapper userRoleMapper;

    /**
     * BCrypt 密码加密器
     * BCrypt 是单向哈希算法，每次加密结果不同（含随机 salt），
     * 验证时通过 matches(rawPassword, encodedPassword) 对比
     */
    private final PasswordEncoder passwordEncoder;

    /**
     * Redis 字符串操作模板
     * 用途：
     *  1. 缓存 Access Token：key=token:access:{userId}，TTL=Access Token 有效期
     *  2. Token 黑名单：key=token:blacklist:{token}，TTL=Token 剩余有效期
     */
    private final StringRedisTemplate redisTemplate;

    /**
     * 用户登录
     *
     * <p>完整登录流程：
     * <ol>
     *   <li>调用 {@code authenticationManager.authenticate()} 触发 Spring Security 认证流程：
     *       内部调用 UserDetailsServiceImpl 加载用户，并用 BCrypt 比对密码</li>
     *   <li>认证成功后，从 Authentication 中取出 {@link LoginUser}（含用户实体和权限信息）</li>
     *   <li>构建 JWT Claims（用户 ID、用户名、角色列表），生成 Access Token 和 Refresh Token</li>
     *   <li>将 Access Token 缓存到 Redis（key: token:access:{userId}），
     *       实现单点登录：新登录会覆盖旧的 Token 缓存</li>
     *   <li>构建并返回 {@link LoginResponse}（含双 Token 和用户信息）</li>
     * </ol>
     *
     * @param request 包含 username 和 password 的登录请求
     * @return 登录响应（accessToken、refreshToken、expiresIn、userInfo）
     * @throws org.springframework.security.authentication.BadCredentialsException 密码错误
     * @throws org.springframework.security.authentication.DisabledException 账号被禁用
     */
    @Override
    public LoginResponse login(LoginRequest request) {
        // 第一步：构建 Spring Security 认证请求（未认证状态的 Token）
        // UsernamePasswordAuthenticationToken(principal, credentials) — 未认证版本构造函数
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));

        // 第二步：认证成功，从 Authentication 的 principal 中取出 LoginUser
        // principal 是 UserDetailsServiceImpl.loadUserByUsername() 返回的对象
        LoginUser loginUser = (LoginUser) authentication.getPrincipal();
        SysUser user = loginUser.getUser();

        // 第三步：构建 Access Token 的额外 Claims
        // 将角色列表放入 Token，JwtAuthenticationFilter 解析后无需额外查询角色
        Map<String, Object> claims = new HashMap<>();
        claims.put("roles", loginUser.getRoles());

        // 第四步：生成 Access Token（JWT 30天兜底，实际由 Redis TTL 控制会话有效期）
        String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getUsername(), claims);

        // 第五步：将 Access Token 缓存到 Redis
        // 作用：支持单点登录（后续登录会覆盖），也便于主动失效（删除此 key 即可）
        // TTL 与 Token 有效期一致，确保缓存不会比 Token 更早或更晚失效
        redisTemplate.opsForValue().set(
                "token:access:" + user.getId(),  // Redis key
                accessToken,                      // 缓存的 Token 值
                jwtProperties.getExpiration(),    // TTL 值（毫秒数）
                TimeUnit.MILLISECONDS             // TTL 单位
        );

        // 第六步：将认证信息写入 HTTP session，实现 SSO
        // 这样同一浏览器发起 OAuth2 授权时，Spring Authorization Server 能在 session 中
        // 找到已认证用户，直接跳过 OAuthLogin 密码输入页（复用管理台登录状态）
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        SecurityContextRepository sessionRepo = new HttpSessionSecurityContextRepository();
        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs != null) {
            attrs.getRequest().getSession(true);
            sessionRepo.saveContext(context, attrs.getRequest(), attrs.getResponse());
        }

        // 第七步：组装用户信息 DTO 和登录响应
        UserInfo userInfo = buildUserInfo(user, loginUser);
        return LoginResponse.builder()
                .accessToken(accessToken)
                .userInfo(userInfo)
                .build();
    }

    /**
     * 用户注册
     *
     * <p>注册流程：
     * <ol>
     *   <li>检查用户名唯一性（数据库级别也有唯一索引，此处提前校验给出友好提示）</li>
     *   <li>BCrypt 加密密码并创建用户记录</li>
     *   <li>分配默认角色（id=3，对应"普通用户" USER 角色）</li>
     * </ol>
     *
     * <p>{@code @Transactional} 确保插入用户和分配角色两步操作的原子性：
     * 若角色分配失败，用户创建也会回滚。
     *
     * @param request 注册请求（username、password、email、nickname）
     * @throws BusinessException 用户名已存在时抛出
     */
    @Override
    @Transactional
    public void register(RegisterRequest request) {
        // 第一步：检查用户名是否已被注册
        Long count = userMapper.selectCount(
                new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getUsername, request.getUsername()));
        if (count > 0) {
            throw new BusinessException(400, "用户名已存在，请换一个用户名");
        }

        // 第二步：创建用户实体并设置字段
        SysUser user = new SysUser();
        user.setUsername(request.getUsername());
        // 密码必须加密存储，绝对不能明文入库
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEmail(request.getEmail());
        // nickname 可选，未填写时默认使用 username 作为昵称
        user.setNickname(request.getNickname() != null ? request.getNickname() : request.getUsername());
        // 新注册用户默认状态为正常（1）
        user.setStatus(1);
        userMapper.insert(user);
        // insert 后，user.getId() 会被 MyBatis-Plus 自动填充（useGeneratedKeys）

        // 第三步：为新用户分配默认角色（"普通用户" USER，id 固定为 3）
        // 此处硬编码 id=3 是一个潜在的技术债，更好的做法是查询 roleKey='USER' 的角色
        SysUserRole userRole = new SysUserRole();
        userRole.setUserId(user.getId());
        userRole.setRoleId(3L);
        userRoleMapper.insert(userRole);
    }

    /**
     * 退出登录
     *
     * <p>退出逻辑（双重失效机制）：
     * <ol>
     *   <li>将当前 Token 写入 Redis 黑名单（key: token:blacklist:{token}），
     *       使 {@link com.auth.platform.security.JwtAuthenticationFilter} 能够识别已注销的 Token</li>
     *   <li>删除 Redis 中缓存的用户 Access Token（key: token:access:{userId}）</li>
     * </ol>
     *
     * <p>黑名单 TTL 设计：
     * 黑名单的 TTL 设置为与 Token 本身的有效期相同（jwtProperties.getExpiration()）。
     * 这是一种保守估计——实际上应该只保留到 Token 实际过期的时刻，
     * 但因为 logout 时不知道 Token 已经使用了多少时间，所以用全量有效期作为安全上界。
     * Redis 的自动过期机制会在 TTL 到达后自动删除黑名单记录，不会永久占用内存。
     *
     * @param token 请求头中的原始 Authorization 值（"Bearer xxx" 或直接的 Token）
     */
    @Override
    public void logout(String token) {
        // 剥离 "Bearer " 前缀（如果存在），获取纯 JWT 字符串
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        // 删除 Redis 中的用户 session token → token 立即失效
        if (jwtUtil.validateToken(token)) {
            Long userId = jwtUtil.getUserIdFromToken(token);
            redisTemplate.delete("token:access:" + userId);
        }

        // 第三步：使 HTTP session 失效，清除 SSO 状态
        // 管理台登录时通过 HttpSessionSecurityContextRepository 写入了 session，
        // 退出时必须同步清除，否则 Spring Authorization Server 仍能从 session 中
        // 找到已认证用户，导致 OAuth 登录无需重新输密码（SSO 不正确地持续有效）
        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs != null) {
            jakarta.servlet.http.HttpSession session = attrs.getRequest().getSession(false);
            if (session != null) {
                session.invalidate();
            }
        }
        // 清除当前线程的 SecurityContext，防止后续代码误用已注销的认证信息
        SecurityContextHolder.clearContext();
    }

    /**
     * 将 {@link SysUser} 实体和 {@link LoginUser} 的权限信息组装为 {@link UserInfo} DTO
     *
     * <p>{@link UserInfo} 是返回给前端的用户信息视图对象，包含用户可公开展示的字段
     * 以及当前用户的角色和权限列表（供前端做权限判断和 UI 控制）。
     *
     * @param user      从数据库查询的用户实体（含所有字段包括密码哈希）
     * @param loginUser Spring Security 的 UserDetails 实现（含角色和权限列表）
     * @return 组装完成的用户信息 DTO（不含密码等敏感字段）
     */
    private UserInfo buildUserInfo(SysUser user, LoginUser loginUser) {
        UserInfo info = new UserInfo();
        info.setId(user.getId());
        info.setUsername(user.getUsername());
        info.setEmail(user.getEmail());
        info.setPhone(user.getPhone());
        info.setNickname(user.getNickname());
        info.setAvatar(user.getAvatar());
        info.setStatus(user.getStatus());
        // 角色列表：如 ["SUPER_ADMIN", "USER"]，前端用于超级管理员权限判断
        info.setRoles(loginUser.getRoles());
        // 权限列表：如 ["system:user:query", "product:list"]，前端用于按钮级权限控制
        info.setPermissions(loginUser.getPermissions());
        return info;
    }
}
