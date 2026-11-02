package com.backend.finledger.user.dto;

import com.backend.finledger.user.UserStatus;
import java.time.Instant;
import java.util.Set;

public record UserDto(
    Long id,
    Long companyId,
    String email,
    String fullName,
    UserStatus status,
    Set<String> roles,
    Instant lastLoginAt,
    Instant createdAt
) {}

