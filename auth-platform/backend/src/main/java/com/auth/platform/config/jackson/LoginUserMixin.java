package com.auth.platform.config.jackson;

import com.auth.platform.entity.SysUser;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.util.List;

/**
 * LoginUser 的 Jackson 反序列化 Mixin 配置类
 *
 * <p>用于解决 Spring Authorization Server 将 OAuth2 授权信息持久化到数据库时，
 * 无法正确反序列化自定义 {@code LoginUser} 主体的问题。
 *
 * <p>配置说明：
 * <ul>
 *   <li>{@code @JsonTypeInfo}：在序列化时写入 {@code @class} 字段，
 *       反序列化时通过该字段还原具体类型</li>
 *   <li>{@code @JsonAutoDetect}：只通过字段（而非 getter/setter）访问属性，
 *       兼容 Lombok 生成的类</li>
 *   <li>{@code @JsonIgnoreProperties(ignoreUnknown = true)}：忽略未知字段，
 *       提高版本兼容性</li>
 *   <li>{@code @JsonCreator}：指定带参构造函数用于反序列化，
 *       明确映射 JSON 字段到构造参数</li>
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
public abstract class LoginUserMixin {

    /**
     * 指定 LoginUser 反序列化时使用的构造函数及参数映射
     *
     * @param user        系统用户实体（对应 JSON 中的 "user" 字段）
     * @param roles       用户角色标识列表（对应 JSON 中的 "roles" 字段）
     * @param permissions 用户权限标识列表（对应 JSON 中的 "permissions" 字段）
     */
    @JsonCreator
    LoginUserMixin(
            @JsonProperty("user") SysUser user,
            @JsonProperty("roles") List<String> roles,
            @JsonProperty("permissions") List<String> permissions) {
    }
}
