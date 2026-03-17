package com.auth.platform.service;

import com.auth.platform.dto.*;

public interface AuthService {
    LoginResponse login(LoginRequest request);

    void register(RegisterRequest request);

    void logout(String token);
}
