package com.auth.platform.dto;

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
}
