package com.product.app.dto;

import lombok.Data;

import java.util.List;

@Data
public class UserInfo {
    private Long id;
    private String username;
    private String email;
    private String phone;
    private String nickname;
    private String avatar;
    private Integer status;
    private List<String> roles;
    private List<String> permissions;
    private List<OAuthBinding> oauthBindings;
    private Boolean hasPassword;

    @Data
    public static class OAuthBinding {
        private String provider;
        private String oauthUsername;
    }
}
