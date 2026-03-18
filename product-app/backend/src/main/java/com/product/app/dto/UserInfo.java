package com.product.app.dto;

import lombok.Data;

import java.util.List;

/**
 * 用户信息 DTO，包含用户基本信息、角色列表、权限列表及 OAuth2 绑定信息。
 */
@Data
public class UserInfo {

    /** 用户 ID */
    private Long id;

    /** 用户名 */
    private String username;

    /** 邮箱地址 */
    private String email;

    /** 手机号码 */
    private String phone;

    /** 用户昵称 */
    private String nickname;

    /** 头像地址 */
    private String avatar;

    /** 账号状态：1=正常，0=禁用 */
    private Integer status;

    /** 用户拥有的角色标识列表 */
    private List<String> roles;

    /** 用户拥有的权限标识列表 */
    private List<String> permissions;

    /** 用户已绑定的 OAuth2 账号列表 */
    private List<OAuthBinding> oauthBindings;

    /** 是否已设置密码，未设置密码的用户只能通过 OAuth2 登录 */
    private Boolean hasPassword;

    /**
     * OAuth2 绑定信息内部类，描述某个已绑定的第三方账号。
     */
    @Data
    public static class OAuthBinding {

        /** OAuth2 提供商标识，如 auth-platform */
        private String provider;

        /** 该第三方账号的用户名 */
        private String oauthUsername;
    }
}
