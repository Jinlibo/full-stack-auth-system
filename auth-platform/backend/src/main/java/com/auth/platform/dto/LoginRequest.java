package com.auth.platform.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 用户登录请求数据传输对象
 * <p>封装用户登录时提交的用户名和密码信息，用于传递给认证服务进行身份验证。</p>
 */
@Data
public class LoginRequest {
    /** 用户名，不能为空 */
    @NotBlank(message = "用户名不能为空")
    private String username;
    /** 登录密码，不能为空 */
    @NotBlank(message = "密码不能为空")
    private String password;
}
