package com.backend.finledger.user.dto;

import java.util.Set;

public record RoleDto(
    Long id,
    Long companyId,
    String name,
    String description,
    Set<String> permissions
) {}

