package com.product.app.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PasswordRequest {

    /** 旧密码（已有密码的用户修改时必填；首次设置密码时可为空） */
    private String oldPassword;

    @NotBlank(message = "新密码不能为空")
    @Size(min = 6, message = "密码不能少于6位")
    private String newPassword;
}
