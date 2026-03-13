package com.auth.platform.service;

import com.auth.platform.dto.*;

public interface AuthService {
    LoginResponse login(LoginRequest request);

    void register(RegisterRequest request);

    LoginResponse refreshToken(String refreshToken);

    void logout(String token);
}
