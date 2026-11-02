package com.backend.finledger.sales.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record ReceiptAllocationRequest(
        @NotNull(message = "Invoice ID is required")
        Long invoiceId,

        @NotNull(message = "Allocation amount is required")
        @DecimalMin(value = "0.01", message = "Allocation amount must be greater than zero")
        BigDecimal amount
) {}

