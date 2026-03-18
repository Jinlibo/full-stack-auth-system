package com.auth.platform.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 管理员创建用户请求数据传输对象
 * <p>封装管理员在后台创建新用户时提交的数据，
 * 与用户自主注册（RegisterRequest）的区别在于：可以直接指定用户角色列表。</p>
 */
@Data
public class UserCreateRequest {
    /** 用户名，不能为空，长度限制 3~64 个字符，全局唯一 */
    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 64, message = "用户名长度3-64")
    private String username;
    /** 登录密码（明文），不能为空，长度限制 6~128 个字符，存储时使用 BCrypt 加密 */
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 128, message = "密码长度6-128")
    private String password;
    /** 用户邮箱，可选字段 */
    private String email;
    /** 用户手机号，可选字段 */
    private String phone;
    /** 用户昵称，可选字段，未填写时默认使用用户名 */
    private String nickname;
    /** 要分配给该用户的角色 ID 列表，可为空（创建后无任何角色） */
    private List<Long> roleIds;
}
