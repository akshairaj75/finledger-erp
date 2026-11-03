package com.backend.finledger.expense.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record ExpenseCategoryRequest(
    @NotBlank(message = "Category name is required")
    String name,

    @NotNull(message = "GL expense account is required")
    Long accountId,

    Long defaultTaxRateId,
    BigDecimal limitPerClaim,
    Boolean receiptRequired,
    Boolean isActive
) {}

