package com.backend.finledger.banking.dto;

import java.math.BigDecimal;
import java.util.List;

public record AutoMatchResultDto(
        int matchedCount,
        BigDecimal reconciledTotal,
        BigDecimal remainingDifference,
        List<BankStatementLineDto> matchedLines
) {}

