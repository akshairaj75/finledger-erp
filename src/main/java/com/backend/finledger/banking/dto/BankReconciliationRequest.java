package com.backend.finledger.banking.dto;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record BankReconciliationRequest(
        @NotNull(message = "Bank account ID is required")
        Long bankAccountId,

        @NotNull(message = "Statement date is required")
        LocalDate statementDate,

        @NotNull(message = "Statement balance is required")
        BigDecimal statementBalance
) {}

