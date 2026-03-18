package com.auth.platform.config;

import com.auth.platform.common.OAuth2ClientDefaults;
import com.auth.platform.entity.SysUser;
import com.auth.platform.entity.SysUserRole;
import com.auth.platform.mapper.OAuth2RegisteredClientMapper;
import com.auth.platform.mapper.SysUserMapper;
import com.auth.platform.mapper.SysUserRoleMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

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

    private final OAuth2RegisteredClientMapper oauth2ClientMapper;

    private final SysUserMapper userMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final PasswordEncoder passwordEncoder;
    @Value("${app.product-app-redirect-uri:http://localhost:5174/oauth/callback}")
    private String productAppRedirectUri;

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

            // Assign SUPER_ADMIN role (id=1) — new user has no roles yet, no need to check
            SysUserRole ur = new SysUserRole();
            ur.setUserId(admin.getId());
            ur.setRoleId(1L);
            userRoleMapper.insert(ur);
            log.info("DataInitializer: created admin user with SUPER_ADMIN role");
        }
    }

    private void initProductAppOAuthClient() {
        // Single query: null means client does not exist yet
        String storedSecret = oauth2ClientMapper.selectSecretByClientId(PRODUCT_APP_CLIENT_ID);

        if (storedSecret == null) {
            String encodedSecret = passwordEncoder.encode(PRODUCT_APP_CLIENT_SECRET);
            oauth2ClientMapper.insert(
                    UUID.randomUUID().toString(),
                    PRODUCT_APP_CLIENT_ID,
                    encodedSecret,
                    "示例产品应用",
                    OAuth2ClientDefaults.CLIENT_AUTH_METHODS,
                    OAuth2ClientDefaults.GRANT_TYPES,
                    productAppRedirectUri,
                    OAuth2ClientDefaults.SCOPES,
                    OAuth2ClientDefaults.CLIENT_SETTINGS,
                    OAuth2ClientDefaults.TOKEN_SETTINGS
            );
            log.info("DataInitializer: registered product-app OAuth2 client");
        } else if (!passwordEncoder.matches(PRODUCT_APP_CLIENT_SECRET, storedSecret)) {
            // Ensure the stored secret is BCrypt-encoded (SQL may have wrong hash)
            String encodedSecret = passwordEncoder.encode(PRODUCT_APP_CLIENT_SECRET);
            oauth2ClientMapper.updateSecret(encodedSecret, PRODUCT_APP_CLIENT_ID);
            log.info("DataInitializer: fixed product-app client_secret hash");
        }
    }
}
