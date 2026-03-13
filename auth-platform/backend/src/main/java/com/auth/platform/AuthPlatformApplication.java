package com.auth.platform;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.auth.platform.mapper")
public class AuthPlatformApplication {
    public static void main(String[] args) {
        SpringApplication.run(AuthPlatformApplication.class, args);
    }
}
