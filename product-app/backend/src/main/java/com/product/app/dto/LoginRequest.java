package com.product.app.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 账号密码登录请求 DTO，包含用户名和密码字段。
 */
@Data
public class LoginRequest {

    /** 登录用户名，不能为空 */
    @NotBlank(message = "用户名不能为空")
    private String username;

    /** 登录密码，不能为空 */
    @NotBlank(message = "密码不能为空")
    private String password;
}
