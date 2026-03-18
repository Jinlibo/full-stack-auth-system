package com.product.app.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 登录响应 DTO，包含 JWT 访问令牌、用户信息及 OAuth2 首次登录时的待绑定标识和临时令牌。
 */
@Data
@Builder
public class LoginResponse {

    /** JWT 访问令牌 */
    private String accessToken;

    /** 登录用户的详细信息 */
    private UserInfo userInfo;

    /** OAuth2 首次登录时返回 true，表示需要用户选择绑定已有账号或创建新账号 */
    private Boolean pendingBind;

    /** OAuth2 临时令牌，用于后续调用 oauthCreateNew 或 oauthBindExisting 接口 */
    private String oauthPendingToken;
}
