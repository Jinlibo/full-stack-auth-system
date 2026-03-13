package com.product.app;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.product.app.mapper")
public class ProductAppApplication {
    public static void main(String[] args) {
        SpringApplication.run(ProductAppApplication.class, args);
    }
}
