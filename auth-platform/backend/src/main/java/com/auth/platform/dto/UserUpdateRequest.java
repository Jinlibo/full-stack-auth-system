package com.auth.platform.dto;

import lombok.Data;

import java.util.List;

@Data
public class UserUpdateRequest {
    private String email;
    private String phone;
    private String nickname;
    private String avatar;
    private Integer status;
    private List<Long> roleIds;
}
