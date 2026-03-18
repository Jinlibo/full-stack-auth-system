package com.auth.platform.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.Map;

/**
 * JWT 工具类
 *
 * <p>封装 JSON Web Token（JWT）的生成、解析和验证逻辑。
 * 使用 JJWT（Java JWT）库，算法为 HMAC-SHA256（HS256）。
 *
 * <p>JWT 结构说明：
 * <pre>
 * Header.Payload.Signature
 * - Header：{"alg":"HS256","typ":"JWT"}
 * - Payload（Claims）：
 *     sub（subject）：用户 ID（字符串形式）
 *     username：用户名
 *     roles：角色列表
 *     iss（issuer）：签发方（配置文件中的 jwt.issuer）
 *     iat（issuedAt）：签发时间（Unix 时间戳）
 *     exp（expiration）：过期时间（Unix 时间戳）
 *     type：token 类型，access token 不含此字段，refresh token 值为 "refresh"
 * - Signature：使用 Base64 解码后的密钥对 Header.Payload 做 HMAC-SHA256 签名
 * </pre>
 *
 * <p>密钥安全性：
 * <ul>
 *   <li>密钥存储在配置文件（application.yml）中，生产环境应通过环境变量注入</li>
 *   <li>密钥格式：Base64 编码的字节序列，至少 256 位（32 字节）才能满足 HS256 要求</li>
 *   <li>每次调用 getSigningKey() 从配置动态获取，支持密钥轮换（重启后生效）</li>
 * </ul>
 *
 * @author auth-platform
 */
@Component
@RequiredArgsConstructor
public class JwtUtil {

    /**
     * JWT 配置属性，注入自 application.yml（通过 @ConfigurationProperties 绑定）
     * 包含：secret（密钥）、expiration（Access Token 有效期）、
     * refreshExpiration（Refresh Token 有效期）、issuer（签发方）
     */
    private final JwtProperties jwtProperties;

    /**
     * 获取 HMAC-SHA256 签名密钥
     *
     * <p>将 Base64 编码的密钥字符串解码为字节数组，再封装为 SecretKey 对象。
     * Keys.hmacShaKeyFor() 会根据字节数组长度自动选择合适的 HMAC 算法强度。
     *
     * <p>私有方法：仅供本类内部的 Token 生成和解析方法调用。
     *
     * @return 用于 JWT 签名的 {@link SecretKey} 实例
     */
    private SecretKey getSigningKey() {
        // BASE64 解码：将配置文件中的 Base64 字符串还原为原始字节数组
        byte[] keyBytes = Decoders.BASE64.decode(jwtProperties.getSecret());
        // 将字节数组包装为 SecretKey，JJWT 会自动匹配 HmacSHA256/384/512
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * 生成 Access Token（短期访问令牌）
     *
     * <p>Access Token 携带用户身份信息（用户 ID、用户名、角色），
     * 每次 API 请求都需要在 Authorization 头中携带此 Token。
     * 有效期较短（默认 2 小时），减少 Token 泄露后的风险窗口。
     *
     * <p>Claims 说明：
     * <ul>
     *   <li>sub：用户 ID（String 类型，JWT 规范中 subject 是字符串）</li>
     *   <li>username：用户名（用于 JwtAuthenticationFilter 中加载 UserDetails）</li>
     *   <li>extraClaims：扩展声明，通常包含 roles（角色列表）</li>
     *   <li>iss：签发方（如 "auth-platform"，用于多服务场景区分 Token 来源）</li>
     *   <li>iat：Token 签发时间（当前系统时间）</li>
     *   <li>exp：Token 过期时间（iat + expiration 毫秒数）</li>
     * </ul>
     *
     * @param userId      用户的数据库主键 ID
     * @param username    用户名
     * @param extraClaims 额外的声明信息（如 roles 列表），会合并到 JWT Payload 中
     * @return 签名后的 JWT 字符串（Base64URL 编码的三段式结构：header.payload.signature）
     */
    public String generateAccessToken(Long userId, String username, Map<String, Object> extraClaims) {
        return Jwts.builder()
                // 先设置 extraClaims（如 roles），后设置的核心 claims 不会被覆盖
                .claims(extraClaims)
                // sub claim：存储用户 ID（String 类型）
                .subject(String.valueOf(userId))
                // 自定义 claim：用户名（用于 filter 中按用户名加载权限）
                .claim("username", username)
                // iss claim：标识 Token 的签发方
                .issuer(jwtProperties.getIssuer())
                // iat claim：Token 签发时间
                .issuedAt(new Date())
                // exp claim：使用配置文件中的 expiration（默认 2 小时）
                .expiration(new Date(System.currentTimeMillis() + jwtProperties.getExpiration()))
                // 使用 HMAC-SHA256 签名（signWith 自动识别密钥长度并选择算法）
                .signWith(getSigningKey())
                // 构建并序列化为 Base64URL 编码的 JWT 字符串
                .compact();
    }

    /**
     * 解析 JWT Token，获取 Claims（声明集合）
     *
     * <p>解析过程：
     * <ol>
     *   <li>Base64URL 解码三个部分（header、payload、signature）</li>
     *   <li>使用密钥重新计算签名，与 Token 中的 signature 比对</li>
     *   <li>检查 exp（过期时间）是否已过</li>
     *   <li>返回 Payload 部分的 Claims 对象</li>
     * </ol>
     *
     * <p>若 Token 签名无效或已过期，会抛出 {@link JwtException} 的子类异常：
     * <ul>
     *   <li>{@link ExpiredJwtException}：Token 已过期</li>
     *   <li>{@link MalformedJwtException}：Token 格式错误</li>
     *   <li>{@link io.jsonwebtoken.security.SignatureException}：签名不匹配（Token 被篡改）</li>
     * </ul>
     *
     * @param token JWT 字符串
     * @return 解析后的 {@link Claims} 对象，包含所有 Payload 中的声明
     * @throws JwtException 若 Token 无效、过期或被篡改
     */
    public Claims parseToken(String token) {
        return Jwts.parser()
                // 设置用于验证签名的密钥（必须与生成时使用的密钥一致）
                .verifyWith(getSigningKey())
                .build()
                // 解析签名的 JWT（SignedJWT），验证签名并返回完整的 JwtClaims
                .parseSignedClaims(token)
                // getPayload() 返回 Claims 对象（包含所有声明的 Map-like 接口）
                .getPayload();
    }

    /**
     * 验证 JWT Token 的有效性
     *
     * <p>通过调用 parseToken() 并捕获异常来判断 Token 是否有效。
     * 有效性包含两层含义：
     * <ol>
     *   <li>签名正确（Token 未被篡改）</li>
     *   <li>未过期（当前时间 &lt; exp 时间）</li>
     * </ol>
     *
     * <p>使用场景：
     * <ul>
     *   <li>{@code JwtAuthenticationFilter}：每次请求到来时验证 Token</li>
     *   <li>{@code AuthServiceImpl#logout}：logout 时先验证再加黑名单</li>
     *   <li>{@code AuthServiceImpl#refreshToken}：刷新前验证 Refresh Token</li>
     * </ul>
     *
     * @param token JWT 字符串
     * @return true 表示 Token 有效（签名正确且未过期）；false 表示无效
     */
    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            // JwtException：包含所有 JJWT 解析相关异常（过期、格式错误、签名错误等）
            // IllegalArgumentException：token 为 null 或空字符串时抛出
            return false;
        }
    }

    /**
     * 从 Token 中提取用户 ID
     *
     * <p>JWT 的 sub（subject）claim 存储了用户 ID（字符串形式），
     * 此方法将其解析并转换为 Long 类型返回。
     *
     * @param token 有效的 JWT 字符串（调用前应先通过 validateToken 确认有效）
     * @return 用户 ID（Long 类型）
     * @throws NumberFormatException 若 subject 不是有效的数字字符串（正常情况不会发生）
     */
    public Long getUserIdFromToken(String token) {
        // getSubject() 返回 JWT sub claim 的值（即 generateAccessToken 中设置的 userId 字符串）
        return Long.parseLong(parseToken(token).getSubject());
    }
}
