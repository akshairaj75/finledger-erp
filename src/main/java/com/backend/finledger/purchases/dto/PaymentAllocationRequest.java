package com.backend.finledger.purchases.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record PaymentAllocationRequest(
        @NotNull(message = "Bill ID is required")
        Long billId,

        @NotNull(message = "Allocation amount is required")
        @DecimalMin(value = "0.01", message = "Allocation amount must be greater than zero")
        BigDecimal amount
) {}

