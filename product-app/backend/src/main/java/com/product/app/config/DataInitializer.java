package com.product.app.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.product.app.entity.AppUser;
import com.product.app.entity.AppUserRole;
import com.product.app.mapper.AppUserMapper;
import com.product.app.mapper.AppUserRoleMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 应用启动时的数据初始化器，确保管理员用户（admin）存在且密码哈希正确。
 * SQL 初始化脚本中使用了占位符哈希，此组件在启动时将其替换为正确的 BCrypt 哈希，
 * 保证 admin/admin123 账号始终可用。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    /** 管理员用户名 */
    private static final String ADMIN_USERNAME = "admin";

    /** 管理员默认密码 */
    private static final String ADMIN_PASSWORD = "admin123";

    private final AppUserMapper userMapper;
    private final AppUserRoleMapper userRoleMapper;
    private final PasswordEncoder passwordEncoder;

    /**
     * 应用启动后执行，检查并初始化管理员账号及其角色绑定。
     * 若管理员不存在则创建；若密码哈希不匹配则重新写入正确哈希。
     *
     * @param args Spring Boot 应用启动参数
     */
    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        AppUser admin = userMapper.selectOne(
                new LambdaQueryWrapper<AppUser>().eq(AppUser::getUsername, ADMIN_USERNAME));

        if (admin == null) {
            admin = new AppUser();
            admin.setUsername(ADMIN_USERNAME);
            admin.setPassword(passwordEncoder.encode(ADMIN_PASSWORD));
            admin.setEmail("admin@product.com");
            admin.setNickname("管理员");
            admin.setStatus(1);
            userMapper.insert(admin);
            log.info("DataInitializer: 已创建 product-app 管理员账号");

            Long count = userRoleMapper.selectCount(
                    new LambdaQueryWrapper<AppUserRole>()
                            .eq(AppUserRole::getUserId, admin.getId())
                            .eq(AppUserRole::getRoleId, 1L));
            if (count == 0) {
                AppUserRole ur = new AppUserRole();
                ur.setUserId(admin.getId());
                ur.setRoleId(1L);
                userRoleMapper.insert(ur);
            }
        } else if (admin.getPassword() == null
                || !passwordEncoder.matches(ADMIN_PASSWORD, admin.getPassword())) {
            admin.setPassword(passwordEncoder.encode(ADMIN_PASSWORD));
            userMapper.updateById(admin);
            log.info("DataInitializer: 已修复 product-app 管理员密码哈希");
        }
    }
}
