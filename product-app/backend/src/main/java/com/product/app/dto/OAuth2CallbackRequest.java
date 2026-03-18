package com.product.app.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * OAuth2 授权回调请求 DTO，包含授权码和防 CSRF 的 state 参数。
 */
@Data
public class OAuth2CallbackRequest {

    /** OAuth2 授权码，不能为空 */
    @NotBlank(message = "授权码不能为空")
    private String code;

    /** 防 CSRF 的随机 state 参数 */
    private String state;
}
