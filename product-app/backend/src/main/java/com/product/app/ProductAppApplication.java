package com.product.app;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 产品应用程序主启动类，负责引导整个 Spring Boot 应用并扫描 Mapper 接口。
 */
@SpringBootApplication
@MapperScan("com.product.app.mapper")
public class ProductAppApplication {

    /**
     * 应用程序入口方法。
     *
     * @param args 命令行启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(ProductAppApplication.class, args);
    }
}
