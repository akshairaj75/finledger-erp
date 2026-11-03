package com.backend.finledger.expense.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record ExpenseLineRequest(
    Short lineNo,

    @NotNull(message = "Category is required")
    Long categoryId,

    Long accountId,

    @NotBlank(message = "Description is required")
    String description,

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
    BigDecimal amount,

    Long taxRateId,
    Long costCenterId,
    Long customerId,
    Boolean isBillable
) {}

