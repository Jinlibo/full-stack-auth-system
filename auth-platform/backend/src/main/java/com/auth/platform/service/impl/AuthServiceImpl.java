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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final JwtProperties jwtProperties;
    private final SysUserMapper userMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final PasswordEncoder passwordEncoder;
    private final StringRedisTemplate redisTemplate;

    @Override
    public LoginResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));
        LoginUser loginUser = (LoginUser) authentication.getPrincipal();
        SysUser user = loginUser.getUser();

        Map<String, Object> claims = new HashMap<>();
        claims.put("roles", loginUser.getRoles());
        String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getUsername(), claims);
        String refreshToken = jwtUtil.generateRefreshToken(user.getId());

        // 缓存token到Redis
        redisTemplate.opsForValue().set("token:access:" + user.getId(), accessToken,
                jwtProperties.getExpiration(), TimeUnit.MILLISECONDS);

        UserInfo userInfo = buildUserInfo(user, loginUser);
        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(jwtProperties.getExpiration() / 1000)
                .userInfo(userInfo)
                .build();
    }

    @Override
    @Transactional
    public void register(RegisterRequest request) {
        // 检查用户名
        Long count = userMapper.selectCount(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, request.getUsername()));
        if (count > 0) {
            throw new BusinessException(400, "用户名已存在");
        }

        SysUser user = new SysUser();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEmail(request.getEmail());
        user.setNickname(request.getNickname() != null ? request.getNickname() : request.getUsername());
        user.setStatus(1);
        userMapper.insert(user);

        // 分配默认角色(普通用户, id=3)
        SysUserRole userRole = new SysUserRole();
        userRole.setUserId(user.getId());
        userRole.setRoleId(3L);
        userRoleMapper.insert(userRole);
    }

    @Override
    public LoginResponse refreshToken(String refreshToken) {
        if (!jwtUtil.validateToken(refreshToken)) {
            throw new BusinessException(401, "Refresh Token已过期");
        }
        String type = jwtUtil.parseToken(refreshToken).get("type", String.class);
        if (!"refresh".equals(type)) {
            throw new BusinessException(401, "无效的Refresh Token");
        }
        Long userId = jwtUtil.getUserIdFromToken(refreshToken);
        SysUser user = userMapper.selectById(userId);
        if (user == null || user.getStatus() != 1) {
            throw new BusinessException(401, "用户不存在或已禁用");
        }
        var roles = userMapper.selectRoleKeysByUserId(userId);
        var permissions = userMapper.selectPermissionKeysByUserId(userId);

        Map<String, Object> claims = new HashMap<>();
        claims.put("roles", roles);
        String newAccessToken = jwtUtil.generateAccessToken(userId, user.getUsername(), claims);
        String newRefreshToken = jwtUtil.generateRefreshToken(userId);

        redisTemplate.opsForValue().set("token:access:" + userId, newAccessToken,
                jwtProperties.getExpiration(), TimeUnit.MILLISECONDS);

        UserInfo userInfo = new UserInfo();
        userInfo.setId(user.getId());
        userInfo.setUsername(user.getUsername());
        userInfo.setNickname(user.getNickname());
        userInfo.setRoles(roles);
        userInfo.setPermissions(permissions);

        return LoginResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .expiresIn(jwtProperties.getExpiration() / 1000)
                .userInfo(userInfo)
                .build();
    }

    @Override
    public void logout(String token) {
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        if (jwtUtil.validateToken(token)) {
            Long userId = jwtUtil.getUserIdFromToken(token);
            // 将token加入黑名单
            redisTemplate.opsForValue().set("token:blacklist:" + token, "1",
                    jwtProperties.getExpiration(), TimeUnit.MILLISECONDS);
            redisTemplate.delete("token:access:" + userId);
        }
    }

    private UserInfo buildUserInfo(SysUser user, LoginUser loginUser) {
        UserInfo info = new UserInfo();
        info.setId(user.getId());
        info.setUsername(user.getUsername());
        info.setEmail(user.getEmail());
        info.setPhone(user.getPhone());
        info.setNickname(user.getNickname());
        info.setAvatar(user.getAvatar());
        info.setStatus(user.getStatus());
        info.setRoles(loginUser.getRoles());
        info.setPermissions(loginUser.getPermissions());
        return info;
    }
}
