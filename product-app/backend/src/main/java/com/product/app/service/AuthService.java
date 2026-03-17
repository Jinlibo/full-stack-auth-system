package com.product.app.service;

import com.product.app.dto.*;

public interface AuthService {
    LoginResponse login(LoginRequest request);

    void register(RegisterRequest request);

    LoginResponse oauthLogin(OAuth2CallbackRequest request);

    LoginResponse oauthCreateNew(String oauthPendingToken);

    LoginResponse oauthBindExisting(String oauthPendingToken, String username, String password);

    UserInfo bindOAuth(Long userId, OAuth2CallbackRequest request);

    String getOAuthAuthorizeUrl(String state);

    void logout(String token);
}
