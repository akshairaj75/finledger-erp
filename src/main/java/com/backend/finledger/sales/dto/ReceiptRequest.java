package com.backend.finledger.sales.dto;

import com.backend.finledger.sales.entity.PaymentMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ReceiptRequest(
        @NotNull(message = "Customer ID is required")
        Long customerId,

        @NotNull(message = "Receipt date is required")
        LocalDate receiptDate,

        @NotNull(message = "Bank account ID is required")
        Long bankAccountId,

        @NotNull(message = "Payment method is required")
        PaymentMethod paymentMethod,

        String reference,
        String currencyCode,

        @NotNull(message = "Receipt amount is required")
        @DecimalMin(value = "0.01", message = "Receipt amount must be greater than zero")
        BigDecimal amount,

        @Valid
        List<ReceiptAllocationRequest> allocations
) {}

