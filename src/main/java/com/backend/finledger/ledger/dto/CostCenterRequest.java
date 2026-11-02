package com.backend.finledger.ledger.dto;

import jakarta.validation.constraints.NotBlank;

public record CostCenterRequest(
    @NotBlank(message = "Code is required")
    String code,

    @NotBlank(message = "Name is required")
    String name,

    Long parentId,
    Boolean isActive
) {}
