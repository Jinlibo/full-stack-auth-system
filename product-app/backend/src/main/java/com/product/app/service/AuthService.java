package com.product.app.service;

import com.product.app.dto.*;

public interface AuthService {
    LoginResponse login(LoginRequest request);

    LoginResponse oauthLogin(OAuth2CallbackRequest request);

    UserInfo bindOAuth(Long userId, OAuth2CallbackRequest request);

    String getOAuthAuthorizeUrl(String state);

    void logout(String token);
}
