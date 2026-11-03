package com.backend.finledger.inventory.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record WarehouseRequest(
    @NotBlank(message = "Warehouse code is required")
    @Size(max = 20, message = "Warehouse code cannot exceed 20 characters")
    String code,

    @NotBlank(message = "Warehouse name is required")
    @Size(max = 100, message = "Warehouse name cannot exceed 100 characters")
    String name,

    @Size(max = 500, message = "Address cannot exceed 500 characters")
    String address,

    Long costCenterId,
    Boolean isDefault,
    Boolean isActive
) {}

