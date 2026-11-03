package com.backend.finledger.inventory.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

public record StockTransferRequest(
    LocalDate transferDate,

    @NotNull(message = "Source warehouse ID is required")
    Long fromWarehouseId,

    @NotNull(message = "Destination warehouse ID is required")
    Long toWarehouseId,

    @Size(max = 500, message = "Notes cannot exceed 500 characters")
    String notes,

    @NotEmpty(message = "At least one transfer line is required")
    @Valid
    List<StockTransferLineRequest> lines
) {}

