package com.auth.platform.config.jackson;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * SysUser 的 Jackson 反序列化 Mixin 配置类
 *
 * <p>与 {@link LoginUserMixin} 配合使用，确保 OAuth2 授权信息从数据库读取时，
 * 嵌套在 {@code LoginUser} 中的 {@code SysUser} 对象也能被正确反序列化。
 *
 * <p>SysUser 包含 {@code LocalDateTime} 类型的字段（createdAt、updatedAt），
 * 需同时在 ObjectMapper 中注册 {@code JavaTimeModule} 方能正确处理。
 *
 * <p>配置说明：
 * <ul>
 *   <li>{@code @JsonTypeInfo}：写入并读取 {@code @class} 类型信息，确保多态反序列化正确</li>
 *   <li>{@code @JsonAutoDetect}：通过字段访问属性，不依赖 getter/setter 方法</li>
 *   <li>{@code @JsonIgnoreProperties(ignoreUnknown = true)}：忽略 JSON 中存在但实体中不存在的字段</li>
 * </ul>
 *
 * @author auth-platform
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
@JsonAutoDetect(
        fieldVisibility = JsonAutoDetect.Visibility.ANY,
        getterVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE,
        setterVisibility = JsonAutoDetect.Visibility.NONE)
@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class SysUserMixin {
}
