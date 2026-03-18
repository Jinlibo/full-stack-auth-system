package com.product.app.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 设置或修改密码请求 DTO。
 * 首次设置密码时 oldPassword 可为空；已有密码的用户修改时必须填写旧密码。
 */
@Data
public class PasswordRequest {

    /** 旧密码（已有密码的用户修改时必填；首次设置密码时可为空） */
    private String oldPassword;

    /** 新密码，不能为空且不少于 6 位 */
    @NotBlank(message = "新密码不能为空")
    @Size(min = 6, message = "密码不能少于6位")
    private String newPassword;
}
