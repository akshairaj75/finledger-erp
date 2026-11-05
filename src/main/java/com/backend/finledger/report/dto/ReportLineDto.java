package com.backend.finledger.report.dto;

import java.math.BigDecimal;

public record ReportLineDto(
        Long accountId,
        String accountCode,
        String accountName,
        BigDecimal amount,
        BigDecimal compareAmount,
        Double changePct
) {}

