package com.auth.platform;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 认证平台应用程序入口类
 *
 * <p>基于 Spring Boot 构建的统一认证与授权管理平台，集成了：
 * <ul>
 *   <li>Spring Security：提供 JWT 令牌认证和方法级别权限控制</li>
 *   <li>Spring Authorization Server：实现 OAuth2 授权服务器功能</li>
 *   <li>MyBatis-Plus：简化数据库 CRUD 操作</li>
 *   <li>Redis：用于 Token 黑名单缓存</li>
 * </ul>
 *
 * @author auth-platform
 */
@SpringBootApplication
@MapperScan("com.auth.platform.mapper")
public class AuthPlatformApplication {

    /**
     * 应用程序主方法，启动 Spring Boot 容器
     *
     * @param args 命令行启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(AuthPlatformApplication.class, args);
    }
}
