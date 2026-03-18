package com.product.app.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

/**
 * 用户注册请求 DTO，包含用户名、密码、邮箱及可选昵称。
 */
@Data
public class RegisterRequest {

    /** 用户名，不能为空 */
    @NotBlank(message = "用户名不能为空")
    private String username;

    /** 密码，不能为空且至少 6 位 */
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, message = "密码至少6位")
    private String password;

    /** 邮箱地址，不能为空且格式须合法 */
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;

    /** 用户昵称，可选，未填写时默认使用用户名 */
    private String nickname;

    /** 手机号，可选 */
    private String phone;
}
