package com.product.app.dto;

import lombok.Data;

@Data
public class UserUpdateRequest {
    private String email;
    private String phone;
    private String nickname;
    private String avatar;
}
