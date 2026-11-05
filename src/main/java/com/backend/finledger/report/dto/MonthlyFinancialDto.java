package com.backend.finledger.report.dto;

import java.math.BigDecimal;

public record MonthlyFinancialDto(
        String month,
        BigDecimal revenue,
        BigDecimal expenses
) {}

