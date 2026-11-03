package com.backend.finledger.expense.dto;

import java.math.BigDecimal;

public record ExpenseLineDto(
    Long id,
    Short lineNo,
    Long categoryId,
    String categoryName,
    Long accountId,
    String accountCode,
    String accountName,
    String description,
    BigDecimal amount,
    Long taxRateId,
    String taxRateCode,
    BigDecimal taxAmount,
    Long costCenterId,
    Long customerId,
    Boolean isBillable
) {}

