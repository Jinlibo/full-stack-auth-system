package com.product.app.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;

/**
 * MyBatis-Plus 配置类，注册分页插件及自动填充创建时间、更新时间的元数据处理器。
 */
@Configuration
public class MyBatisPlusConfig {

    /**
     * 注册 MyBatis-Plus 拦截器，添加 MySQL 分页插件。
     *
     * @return 配置完毕的 MybatisPlusInterceptor 实例
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor i = new MybatisPlusInterceptor();
        i.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return i;
    }

    /**
     * 注册元数据对象处理器，在新增时自动填充 createdAt 和 updatedAt，
     * 在更新时自动填充 updatedAt 字段。
     *
     * @return MetaObjectHandler 实例
     */
    @Bean
    public MetaObjectHandler metaObjectHandler() {
        return new MetaObjectHandler() {
            /**
             * 新增时自动填充创建时间和更新时间。
             *
             * @param meta 元数据对象
             */
            @Override
            public void insertFill(MetaObject meta) {
                strictInsertFill(meta, "createdAt", LocalDateTime::now, LocalDateTime.class);
                strictInsertFill(meta, "updatedAt", LocalDateTime::now, LocalDateTime.class);
            }

            /**
             * 更新时自动填充更新时间。
             *
             * @param meta 元数据对象
             */
            @Override
            public void updateFill(MetaObject meta) {
                strictUpdateFill(meta, "updatedAt", LocalDateTime::now, LocalDateTime.class);
            }
        };
    }
}
