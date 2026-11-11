package com.backend.finledger.security.service;

import com.backend.finledger.security.dto.*;

public interface AuthService {
    AuthResponse login(LoginRequest request);
    TokenResponse refresh(RefreshRequest request);
    void logout(String refreshToken);
    MeResponse getMe();
    void changePassword(ChangePasswordRequest request);
}

