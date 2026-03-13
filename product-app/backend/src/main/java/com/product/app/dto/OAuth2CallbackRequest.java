package com.product.app.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class OAuth2CallbackRequest {
    @NotBlank(message = "授权码不能为空")
    private String code;
    private String state;
}
