package com.backend.finledger.expense.dto;

import java.math.BigDecimal;

public record ExpenseCategoryDto(
    Long id,
    Long companyId,
    String name,
    Long accountId,
    String accountCode,
    String accountName,
    Long defaultTaxRateId,
    BigDecimal limitPerClaim,
    Boolean receiptRequired,
    Boolean isActive
) {}

