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

@RestController
@RequestMapping("/api/oauth2")
@RequiredArgsConstructor
public class OAuth2UserInfoController {

    private final SysUserMapper userMapper;
    /**
     * The RSA-based JwtDecoder from AuthorizationServerConfig, used to validate
     * OAuth2 access tokens issued by this authorization server.
     * Cannot use @AuthenticationPrincipal Jwt here because the API security chain
     * uses a custom HMAC JwtAuthenticationFilter and does not configure
     * oauth2ResourceServer, so the injection would always yield null.
     */
    private final JwtDecoder jwtDecoder;

    /**
     * OAuth2 UserInfo endpoint - called by client applications to retrieve
     * the authenticated user's profile using the OAuth2 access_token.
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

        // Spring Authorization Server sets sub to the principal_name (username)
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
