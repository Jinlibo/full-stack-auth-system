package com.auth.platform.controller;

import com.auth.platform.entity.SysUser;
import com.auth.platform.mapper.SysUserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * OAuth2 用户信息端点控制器
 *
 * <p>实现 OAuth2/OIDC 标准的 UserInfo 端点，供已获得访问令牌的第三方客户端应用
 * 调用以获取已认证用户的基本信息。
 *
 * <p>请求方式：
 * <pre>GET /api/oauth2/userinfo
 * Authorization: Bearer {access_token}</pre>
 *
 * <p>此端点已在 SecurityConfig 中配置为 permitAll（无需 JWT 登录认证），
 * 但需要在请求头中携带有效的 OAuth2 访问令牌（RSA 签名的 JWT）。
 *
 * @author auth-platform
 */
@RestController
@RequestMapping("/api/oauth2")
@RequiredArgsConstructor
public class OAuth2UserInfoController {

    /** 用户 Mapper，根据用户名查询用户信息 */
    private final SysUserMapper userMapper;

    /**
     * 基于 RSA 密钥的 JWT 解码器（来自 AuthorizationServerConfig），
     * 用于验证授权服务器颁发的 OAuth2 访问令牌签名。
     *
     * <p>注意：此处不能使用 {@code @AuthenticationPrincipal Jwt} 注入，
     * 因为 API 安全过滤链使用自定义 HMAC JwtAuthenticationFilter，
     * 未配置 oauth2ResourceServer，注入结果始终为 null。
     * 因此改为手动注入 JwtDecoder 并在方法内解析 Token。
     */
    private final JwtDecoder jwtDecoder;

    /**
     * OAuth2 UserInfo 端点
     *
     * <p>客户端应用使用 OAuth2 访问令牌调用此接口，获取已授权用户的基本信息。
     * 返回字段遵循 OIDC 规范，包含 sub、username、nickname、email、avatar、phone。
     *
     * <p>处理流程：
     * <ol>
     *   <li>从请求头 Authorization 中提取 Bearer Token</li>
     *   <li>使用 RSA 公钥解码并验证 JWT 签名和有效期</li>
     *   <li>从 JWT 的 sub 字段（即用户名）查询数据库获取用户详情</li>
     *   <li>返回用户基本信息 Map</li>
     * </ol>
     *
     * @param request HTTP 请求，用于提取 Authorization 请求头
     * @return 用户信息 Map；Token 无效返回 401；用户不存在返回 404
     */
    @GetMapping("/userinfo")
    public ResponseEntity<Map<String, Object>> userinfo(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(401).build();
        }
        String token = authHeader.substring(7);

        Jwt jwt;
        try {
            jwt = jwtDecoder.decode(token);
        } catch (JwtException e) {
            return ResponseEntity.status(401).build();
        }

        // Spring Authorization Server 将 principal_name（用户名）设置为 JWT 的 sub 字段
        String username = jwt.getSubject();
        SysUser user = userMapper.selectOne(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username));
        if (user == null) {
            return ResponseEntity.notFound().build();
        }

        Map<String, Object> info = new HashMap<>();
        info.put("sub", String.valueOf(user.getId()));
        info.put("username", user.getUsername());
        info.put("nickname", user.getNickname());
        info.put("email", user.getEmail());
        info.put("avatar", user.getAvatar());
        info.put("phone", user.getPhone());
        return ResponseEntity.ok(info);
    }
}
