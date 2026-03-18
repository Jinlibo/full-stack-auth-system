package com.product.app.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.Map;

/**
 * JWT 工具类，提供 JWT 的生成、解析、验证及用户 ID 提取功能。
 * 使用 HMAC-SHA 算法对令牌进行签名，密钥从配置文件读取。
 */
@Component
public class JwtUtil {

    /** JWT 签名密钥（Base64 编码） */
    @Value("${app.jwt.secret}")
    private String secret;

    /** JWT 过期时间，单位毫秒 */
    @Value("${app.jwt.expiration}")
    private Long expiration;

    /** JWT 签发者标识 */
    @Value("${app.jwt.issuer}")
    private String issuer;

    /**
     * 根据配置的 Base64 密钥构建 HMAC-SHA 签名密钥。
     *
     * @return SecretKey 签名密钥
     */
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
    }

    /**
     * 生成 JWT 访问令牌，包含用户 ID（subject）、用户名及自定义附加信息。
     *
     * @param userId   用户 ID
     * @param username 用户名
     * @param extra    附加声明，如角色列表
     * @return 签名后的 JWT 字符串
     */
    public String generateAccessToken(Long userId, String username, Map<String, Object> extra) {
        return Jwts.builder()
                .claims(extra)
                .subject(String.valueOf(userId))
                .claim("username", username)
                .issuer(issuer)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * 解析 JWT 令牌并返回其载荷（Claims）。
     *
     * @param token JWT 字符串
     * @return JWT 载荷
     * @throws Exception 令牌无效或已过期时抛出异常
     */
    public Claims parseToken(String token) {
        return Jwts.parser().verifyWith(getSigningKey()).build().parseSignedClaims(token).getPayload();
    }

    /**
     * 验证 JWT 令牌是否有效（签名正确且未过期）。
     *
     * @param token JWT 字符串
     * @return true=有效，false=无效或已过期
     */
    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 从 JWT 令牌中提取用户 ID。
     *
     * @param token JWT 字符串
     * @return 用户 ID
     */
    public Long getUserIdFromToken(String token) {
        return Long.parseLong(parseToken(token).getSubject());
    }

    /**
     * 获取 JWT 过期时间（毫秒）。
     *
     * @return 过期时间，单位毫秒
     */
    public long getExpiration() {
        return expiration;
    }
}
