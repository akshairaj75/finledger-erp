package com.backend.finledger.user.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.Set;

public record RoleRequest(
    @NotBlank(message = "Role name is required")
    String name,

    String description,

    Set<String> permissionCodes
) {}

