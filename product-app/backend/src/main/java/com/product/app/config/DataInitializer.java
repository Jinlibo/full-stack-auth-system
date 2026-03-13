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
 * Ensures the admin user exists with the correct password hash on startup.
 * The SQL init script ships with a placeholder BCrypt hash; this component
 * replaces it so admin/admin123 always works.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private static final String ADMIN_USERNAME = "admin";
    private static final String ADMIN_PASSWORD = "admin123";
    private final AppUserMapper userMapper;
    private final AppUserRoleMapper userRoleMapper;
    private final PasswordEncoder passwordEncoder;

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
            log.info("DataInitializer: created product-app admin user");

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
            log.info("DataInitializer: fixed product-app admin password hash");
        }
    }
}
