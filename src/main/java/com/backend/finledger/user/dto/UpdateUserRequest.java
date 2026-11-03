package com.backend.finledger.user.dto;

import com.backend.finledger.user.entity.UserStatus;
import jakarta.validation.constraints.NotBlank;
import java.util.Set;

public record UpdateUserRequest(
    @NotBlank(message = "Full name is required")
    String fullName,

    UserStatus status,

    Set<Long> roleIds
) {}

