package com.backend.finledger.sales.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record InvoiceLineRequest(
        Short lineNo,
        Long itemId,

        @NotBlank(message = "Description is required")
        String description,

        @NotNull(message = "Quantity is required")
        @DecimalMin(value = "0.0001", message = "Quantity must be greater than zero")
        BigDecimal quantity,

        @NotNull(message = "Unit price is required")
        @DecimalMin(value = "0.00", message = "Unit price cannot be negative")
        BigDecimal unitPrice,

        BigDecimal discountPct,
        Long taxRateId,

        @NotNull(message = "Account ID is required")
        Long accountId,

        Long warehouseId,
        Long costCenterId
) {}

