package com.auth.platform.config;

import com.auth.platform.entity.SysUser;
import com.auth.platform.entity.SysUserRole;
import com.auth.platform.mapper.SysUserMapper;
import com.auth.platform.mapper.SysUserRoleMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Initializes required baseline data on startup.
 * <p>
 * Specifically:
 * 1. Ensures the admin user exists with the correct password hash.
 * The SQL schema ships with a placeholder BCrypt hash; this component
 * replaces it with a freshly encoded hash so admin/admin123 always works.
 * 2. Ensures the product-app OAuth2 client is registered in oauth2_registered_client
 * with a valid BCrypt-encoded secret (client_secret_basic/post authentication
 * requires the stored secret to be BCrypt-encoded by Spring Authorization Server).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private static final String ADMIN_USERNAME = "admin";
    private static final String ADMIN_PASSWORD = "admin123";
    private static final String PRODUCT_APP_CLIENT_ID = "product-app";
    private static final String PRODUCT_APP_CLIENT_SECRET = "admin123";
    private final SysUserMapper userMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbcTemplate;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        initAdminUser();
        initProductAppOAuthClient();
    }

    private void initAdminUser() {
        SysUser admin = userMapper.selectOne(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, ADMIN_USERNAME));

        if (admin == null) {
            admin = new SysUser();
            admin.setUsername(ADMIN_USERNAME);
            admin.setPassword(passwordEncoder.encode(ADMIN_PASSWORD));
            admin.setEmail("admin@example.com");
            admin.setNickname("超级管理员");
            admin.setStatus(1);
            userMapper.insert(admin);
            log.info("DataInitializer: created admin user");

            // Assign SUPER_ADMIN role (id=1)
            Long count = userRoleMapper.selectCount(
                    new LambdaQueryWrapper<SysUserRole>()
                            .eq(SysUserRole::getUserId, admin.getId())
                            .eq(SysUserRole::getRoleId, 1L));
            if (count == 0) {
                SysUserRole ur = new SysUserRole();
                ur.setUserId(admin.getId());
                ur.setRoleId(1L);
                userRoleMapper.insert(ur);
            }
        } else if (!passwordEncoder.matches(ADMIN_PASSWORD, admin.getPassword())) {
            // Hash in DB does not match admin123 — fix it
            admin.setPassword(passwordEncoder.encode(ADMIN_PASSWORD));
            userMapper.updateById(admin);
            log.info("DataInitializer: fixed admin password hash");
        }
    }

    private void initProductAppOAuthClient() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM oauth2_registered_client WHERE client_id = ?",
                Integer.class, PRODUCT_APP_CLIENT_ID);

        if (count == null || count == 0) {
            String encodedSecret = passwordEncoder.encode(PRODUCT_APP_CLIENT_SECRET);
            jdbcTemplate.update(
                    "INSERT INTO oauth2_registered_client " +
                            "(id, client_id, client_secret, client_name, " +
                            "client_authentication_methods, authorization_grant_types, " +
                            "redirect_uris, scopes, client_settings, token_settings) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    java.util.UUID.randomUUID().toString(),
                    PRODUCT_APP_CLIENT_ID,
                    encodedSecret,
                    "示例产品应用",
                    "client_secret_basic,client_secret_post",
                    "authorization_code,refresh_token",
                    "http://localhost:5174/oauth/callback",
                    "openid,profile,email",
                    "{\"@class\":\"java.util.Collections$UnmodifiableMap\"," +
                            "\"settings.client.require-proof-key\":false," +
                            "\"settings.client.require-authorization-consent\":true}",
                    "{\"@class\":\"java.util.Collections$UnmodifiableMap\"," +
                            "\"settings.token.reuse-refresh-tokens\":true," +
                            "\"settings.token.id-token-signature-algorithm\":[\"org.springframework.security.oauth2.jose.jws.SignatureAlgorithm\",\"RS256\"]," +
                            "\"settings.token.access-token-time-to-live\":[\"java.time.Duration\",3600.000000000]," +
                            "\"settings.token.access-token-format\":{\"@class\":\"org.springframework.security.oauth2.server.authorization.settings.OAuth2TokenFormat\",\"value\":\"self-contained\"}," +
                            "\"settings.token.refresh-token-time-to-live\":[\"java.time.Duration\",86400.000000000]," +
                            "\"settings.token.authorization-code-time-to-live\":[\"java.time.Duration\",300.000000000]," +
                            "\"settings.token.device-code-time-to-live\":[\"java.time.Duration\",300.000000000]}"
            );
            log.info("DataInitializer: registered product-app OAuth2 client");
        } else {
            // Ensure the stored secret is BCrypt-encoded (SQL may have wrong hash)
            String storedSecret = jdbcTemplate.queryForObject(
                    "SELECT client_secret FROM oauth2_registered_client WHERE client_id = ?",
                    String.class, PRODUCT_APP_CLIENT_ID);
            if (storedSecret != null && !passwordEncoder.matches(PRODUCT_APP_CLIENT_SECRET, storedSecret)) {
                String encodedSecret = passwordEncoder.encode(PRODUCT_APP_CLIENT_SECRET);
                jdbcTemplate.update(
                        "UPDATE oauth2_registered_client SET client_secret = ? WHERE client_id = ?",
                        encodedSecret, PRODUCT_APP_CLIENT_ID);
                log.info("DataInitializer: fixed product-app client_secret hash");
            }
        }
    }
}
