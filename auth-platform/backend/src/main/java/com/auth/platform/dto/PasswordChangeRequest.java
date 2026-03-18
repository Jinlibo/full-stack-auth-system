package com.auth.platform.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 用户修改密码请求数据传输对象
 * <p>封装用户修改密码时需要提交的旧密码和新密码信息，
 * 服务层会先验证旧密码正确性，再将新密码加密后更新。</p>
 */
@Data
public class PasswordChangeRequest {
    /** 当前旧密码（明文），用于验证用户身份，不能为空 */
    @NotBlank(message = "旧密码不能为空")
    private String oldPassword;
    /** 新密码（明文），验证通过后将使用 BCrypt 加密存储，不能为空 */
    @NotBlank(message = "新密码不能为空")
    private String newPassword;
}
