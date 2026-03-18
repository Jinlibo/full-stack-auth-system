package com.auth.platform.service;

import com.auth.platform.dto.*;

/**
 * 认证服务接口
 * <p>定义用户认证相关的核心业务方法，包括登录、注册和退出登录功能。</p>
 */
public interface AuthService {

    /**
     * 用户登录
     * <p>验证用户名和密码，认证成功后生成 JWT 访问令牌并缓存至 Redis，
     * 返回令牌和用户基本信息。</p>
     *
     * @param request 包含用户名和密码的登录请求
     * @return 登录响应（包含 accessToken 和用户信息）
     */
    LoginResponse login(LoginRequest request);

    /**
     * 用户注册
     * <p>创建新用户账号，密码使用 BCrypt 加密存储，并自动分配默认角色。</p>
     *
     * @param request 包含用户名、密码、邮箱等信息的注册请求
     */
    void register(RegisterRequest request);

    /**
     * 用户退出登录
     * <p>使当前 JWT 令牌失效（删除 Redis 缓存），并清除 HTTP Session 中的认证信息。</p>
     *
     * @param token 请求头中的 Authorization 值（"Bearer xxx" 或直接的 Token 字符串）
     */
    void logout(String token);
}
