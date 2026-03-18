package com.auth.platform.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 用户注册请求数据传输对象
 * <p>封装用户自主注册时提交的信息，注册成功后系统将自动分配默认的"普通用户"角色。</p>
 */
@Data
public class RegisterRequest {
    /** 用户名，不能为空，长度限制 3~64 个字符 */
    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 64)
    private String username;
    /** 登录密码（明文），不能为空，长度限制 6~128 个字符，存储时会使用 BCrypt 加密 */
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 128)
    private String password;
    /** 用户邮箱，可选字段 */
    private String email;
    /** 用户昵称，可选字段，未填写时默认使用用户名 */
    private String nickname;
}
