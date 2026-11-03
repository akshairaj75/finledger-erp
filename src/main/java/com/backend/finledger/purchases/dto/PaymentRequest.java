package com.backend.finledger.purchases.dto;

import com.backend.finledger.sales.entity.PaymentMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record PaymentRequest(
        @NotNull(message = "Vendor ID is required")
        Long vendorId,

        @NotNull(message = "Payment date is required")
        LocalDate paymentDate,

        @NotNull(message = "Bank account ID is required")
        Long bankAccountId,

        @NotNull(message = "Payment method is required")
        PaymentMethod paymentMethod,

        @Size(max = 60, message = "Reference cannot exceed 60 characters")
        String reference,

        @Size(min = 3, max = 3, message = "Currency code must be 3 characters")
        String currencyCode,

        @NotNull(message = "Payment amount is required")
        @DecimalMin(value = "0.01", message = "Payment amount must be greater than zero")
        BigDecimal amount,

        @Valid
        List<PaymentAllocationRequest> allocations
) {}

