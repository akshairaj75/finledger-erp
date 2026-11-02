package com.backend.finledger.security.dto;

public record TokenResponse(
    String accessToken,
    String refreshToken
) {}

