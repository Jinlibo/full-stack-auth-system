package com.auth.platform.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 用户登录响应数据传输对象
 * <p>登录成功后返回给前端的响应体，包含访问令牌和当前登录用户的基本信息。</p>
 */
@Data
@Builder
public class LoginResponse {
    /** JWT 访问令牌，前端后续请求需在 Authorization 头中携带此 Token */
    private String accessToken;
    /** 当前登录用户信息（包含用户基本资料、角色列表和权限列表） */
    private UserInfo userInfo;
}
