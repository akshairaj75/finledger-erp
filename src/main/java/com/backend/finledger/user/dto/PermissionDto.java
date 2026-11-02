package com.backend.finledger.user.dto;

public record PermissionDto(
    Long id,
    String code,
    String module,
    String description
) {}

