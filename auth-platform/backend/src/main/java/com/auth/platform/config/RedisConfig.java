package com.auth.platform.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis 配置类
 *
 * <p>自定义 {@link RedisTemplate} 的序列化策略，解决默认的 JDK 序列化导致的
 * Redis 键值可读性差、跨语言兼容性差等问题。
 *
 * <p>序列化配置：
 * <ul>
 *   <li>Key / Hash Key：使用 {@link StringRedisSerializer}，以纯字符串形式存储，Redis CLI 中直接可读</li>
 *   <li>Value / Hash Value：使用 {@link GenericJackson2JsonRedisSerializer}，以 JSON 格式存储，
 *       支持任意对象类型，反序列化时通过 JSON 中的 {@code @class} 字段还原原始类型</li>
 * </ul>
 *
 * @author auth-platform
 */
@Configuration
public class RedisConfig {

    /**
     * 注册自定义序列化配置的 RedisTemplate Bean
     *
     * <p>配置了 Key 和 Value 的序列化器后，使用此 Template 存储的数据
     * 在 Redis 中以人类可读的字符串键和 JSON 值存储，方便调试和监控。
     *
     * @param factory Redis 连接工厂，由 Spring Boot 自动配置注入
     * @return 配置好序列化策略的 RedisTemplate 实例
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);
        // 字符串序列化器：用于 Key，避免 Redis 中出现乱码前缀
        template.setKeySerializer(new StringRedisSerializer());
        // JSON 序列化器：用于 Value，支持复杂对象类型
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        // Hash 结构的 Key 同样使用字符串序列化
        template.setHashKeySerializer(new StringRedisSerializer());
        // Hash 结构的 Value 同样使用 JSON 序列化
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.afterPropertiesSet();
        return template;
    }
}
