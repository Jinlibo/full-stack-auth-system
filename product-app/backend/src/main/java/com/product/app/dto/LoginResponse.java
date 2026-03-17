package com.product.app.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LoginResponse {
    private String accessToken;
    private String refreshToken;
    private Long expiresIn;
    private UserInfo userInfo;
    // OAuth 首次登录时返回，表示需要用户选择绑定已有账号或创建新账号
    private Boolean pendingBind;
    // 临时令牌，用于后续 oauthCreateNew 或 oauthBindExisting
    private String oauthPendingToken;
}
