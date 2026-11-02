package com.backend.finledger.security.dto;

import com.backend.finledger.user.dto.UserDto;
import java.util.Set;

public record AuthResponse(
    String accessToken,
    String refreshToken,
    UserDto user,
    Set<String> permissions
) {}

