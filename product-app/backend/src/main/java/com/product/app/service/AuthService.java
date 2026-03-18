package com.product.app.service;

import com.product.app.dto.*;

/**
 * 认证服务接口，定义账号密码登录、用户注册、OAuth2 登录流程及退出登录等认证相关操作。
 */
public interface AuthService {

    /**
     * 账号密码登录，验证通过后颁发 JWT 令牌。
     *
     * @param request 登录请求，包含用户名和密码
     * @return 登录响应，包含 accessToken 和用户信息
     */
    LoginResponse login(LoginRequest request);

    /**
     * 用户注册，创建新账号并分配默认角色。
     *
     * @param request 注册请求，包含用户名、密码、邮箱等信息
     */
    void register(RegisterRequest request);

    /**
     * OAuth2 登录回调，使用授权码换取令牌并获取用户信息。
     * 已绑定用户直接登录；首次登录返回待绑定响应。
     *
     * @param request OAuth2 回调请求，包含授权码和 state
     * @return 登录响应或待绑定响应
     */
    LoginResponse oauthLogin(OAuth2CallbackRequest request);

    /**
     * OAuth2 首次登录后创建新本地账号，并与 OAuth2 账号绑定。
     *
     * @param oauthPendingToken Redis 中存储的临时授权令牌
     * @return 登录响应，包含新账号的 JWT 和用户信息
     */
    LoginResponse oauthCreateNew(String oauthPendingToken);

    /**
     * OAuth2 首次登录后绑定已有本地账号，验证账号凭证后完成绑定。
     *
     * @param oauthPendingToken Redis 中存储的临时授权令牌
     * @param username          已有本地账号的用户名
     * @param password          已有本地账号的密码
     * @return 登录响应，包含已有账号的 JWT 和用户信息
     */
    LoginResponse oauthBindExisting(String oauthPendingToken, String username, String password);

    /**
     * 将 OAuth2 账号绑定到当前已登录用户。
     *
     * @param userId  当前登录用户 ID
     * @param request OAuth2 回调请求，包含授权码和 state
     * @return 绑定后的最新用户信息
     */
    UserInfo bindOAuth(Long userId, OAuth2CallbackRequest request);

    /**
     * 生成 OAuth2 授权地址，供前端跳转至授权服务器发起授权流程。
     *
     * @param state 前端传入的随机 state 参数，用于防 CSRF
     * @return OAuth2 授权地址
     */
    String getOAuthAuthorizeUrl(String state);

    /**
     * 退出登录，从 Redis 中删除 JWT 令牌使其立即失效。
     *
     * @param token 请求头中的 Bearer Token 字符串
     */
    void logout(String token);
}
