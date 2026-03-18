package com.auth.platform.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;

/**
 * MyBatis-Plus 全局配置类
 *
 * <p>配置 MyBatis-Plus 的核心插件和自动填充处理器：
 * <ul>
 *   <li>分页插件：自动处理 MySQL 的分页查询，无需手动编写 LIMIT 语句</li>
 *   <li>自动填充：在插入和更新时自动设置 createdAt 和 updatedAt 字段</li>
 * </ul>
 *
 * @author auth-platform
 */
@Configuration
public class MyBatisPlusConfig {

    /**
     * 注册 MyBatis-Plus 拦截器 Bean
     *
     * <p>添加了 {@link PaginationInnerInterceptor} 分页插件，
     * 指定数据库类型为 MySQL，使 MyBatis-Plus 的 {@code page()} 方法
     * 能够自动生成正确的分页 SQL（LIMIT offset, size）。
     *
     * @return 配置了分页插件的 MyBatis-Plus 拦截器
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }

    /**
     * 注册 MyBatis-Plus 字段自动填充处理器 Bean
     *
     * <p>配合实体类上的 {@code @TableField(fill = FieldFill.INSERT)} 和
     * {@code @TableField(fill = FieldFill.INSERT_UPDATE)} 注解使用：
     * <ul>
     *   <li>插入时：自动填充 {@code createdAt} 和 {@code updatedAt} 为当前时间</li>
     *   <li>更新时：自动填充 {@code updatedAt} 为当前时间</li>
     * </ul>
     *
     * @return 实现了时间字段自动填充的 MetaObjectHandler
     */
    @Bean
    public MetaObjectHandler metaObjectHandler() {
        return new MetaObjectHandler() {
            /**
             * 插入操作时的字段自动填充逻辑
             *
             * @param metaObject MyBatis 元对象，封装了实体类的字段访问
             */
            @Override
            public void insertFill(MetaObject metaObject) {
                this.strictInsertFill(metaObject, "createdAt", LocalDateTime::now, LocalDateTime.class);
                this.strictInsertFill(metaObject, "updatedAt", LocalDateTime::now, LocalDateTime.class);
            }

            /**
             * 更新操作时的字段自动填充逻辑
             *
             * @param metaObject MyBatis 元对象
             */
            @Override
            public void updateFill(MetaObject metaObject) {
                this.strictUpdateFill(metaObject, "updatedAt", LocalDateTime::now, LocalDateTime.class);
            }
        };
    }
}
