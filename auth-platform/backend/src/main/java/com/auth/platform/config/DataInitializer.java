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
 * 应用启动时的基础数据初始化器
 *
 * <p>实现 {@link ApplicationRunner}，在 Spring Boot 完成启动后自动执行，负责：
 * <ol>
 *   <li>确保管理员用户存在且密码哈希正确。SQL 初始化脚本中携带占位哈希，
 *       本组件会在启动时重新编码，确保 admin/admin123 始终可用。</li>
 *   <li>确保 product-app OAuth2 客户端已在 oauth2_registered_client 表中注册，
 *       且客户端密钥（client_secret）为有效的 BCrypt 编码格式
 *       （Spring Authorization Server 的 client_secret_basic/post 认证要求存储的密钥为 BCrypt 编码）。</li>
 * </ol>
 *
 * @author auth-platform
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    /** 管理员默认用户名 */
    private static final String ADMIN_USERNAME = "admin";

    /** 管理员默认密码（明文，BCrypt 编码后存储） */
    private static final String ADMIN_PASSWORD = "admin123";

    /** 示例产品应用的 OAuth2 Client ID */
    private static final String PRODUCT_APP_CLIENT_ID = "product-app";

    /** 示例产品应用的 OAuth2 Client Secret（明文，BCrypt 编码后存储） */
    private static final String PRODUCT_APP_CLIENT_SECRET = "admin123";

    private final OAuth2RegisteredClientMapper oauth2ClientMapper;

    private final SysUserMapper userMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final PasswordEncoder passwordEncoder;
    @Value("${app.product-app-redirect-uri:http://localhost:5174/oauth/callback}")
    private String productAppRedirectUri;

    /**
     * 应用启动后执行初始化逻辑
     *
     * <p>按顺序执行管理员用户初始化和 OAuth2 客户端初始化，
     * 整个方法在同一事务中执行，任意步骤失败均回滚。
     *
     * @param args 启动参数（不使用）
     */
    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        initAdminUser();
        initProductAppOAuthClient();
    }

    /**
     * 初始化管理员用户
     *
     * <p>若数据库中不存在 admin 用户，则创建默认管理员账号并分配超级管理员角色（id=1）。
     * 若已存在则跳过，不做任何修改。
     */
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

            // 分配超级管理员角色（id=1）——新用户尚无角色，无需检查重复
            SysUserRole ur = new SysUserRole();
            ur.setUserId(admin.getId());
            ur.setRoleId(1L);
            userRoleMapper.insert(ur);
            log.info("DataInitializer: 已创建管理员用户并分配超级管理员角色");
        }
    }

    /**
     * 初始化示例产品应用的 OAuth2 客户端
     *
     * <p>处理逻辑：
     * <ul>
     *   <li>若客户端不存在：插入新的客户端注册记录（含 BCrypt 编码的 secret）</li>
     *   <li>若客户端存在但 secret 哈希不匹配（如 SQL 脚本写入了错误哈希）：更新为正确的 BCrypt 编码</li>
     *   <li>若客户端存在且 secret 正确：跳过，不做任何操作</li>
     * </ul>
     */
    private void initProductAppOAuthClient() {
        // 单次查询：返回 null 表示客户端尚未注册
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
            log.info("DataInitializer: 已注册 product-app OAuth2 客户端");
        } else if (!passwordEncoder.matches(PRODUCT_APP_CLIENT_SECRET, storedSecret)) {
            // 存储的 secret 不是有效的 BCrypt 编码（SQL 脚本可能写入了错误哈希），修复之
            String encodedSecret = passwordEncoder.encode(PRODUCT_APP_CLIENT_SECRET);
            oauth2ClientMapper.updateSecret(encodedSecret, PRODUCT_APP_CLIENT_ID);
            log.info("DataInitializer: 已修复 product-app 客户端密钥哈希");
        }
    }
}
