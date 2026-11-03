package com.backend.finledger.inventory.dto;

import com.backend.finledger.inventory.entity.AdjustmentReason;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

public record StockAdjustmentRequest(
    LocalDate adjustmentDate,

    @NotNull(message = "Warehouse ID is required")
    Long warehouseId,

    @NotNull(message = "Adjustment reason is required")
    AdjustmentReason reason,

    @NotNull(message = "Adjustment account ID is required")
    Long adjustmentAccountId,

    @Size(max = 500, message = "Notes cannot exceed 500 characters")
    String notes,

    @NotEmpty(message = "At least one adjustment line is required")
    @Valid
    List<StockAdjustmentLineRequest> lines
) {}

