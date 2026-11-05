package com.backend.finledger.report.dto;

import java.math.BigDecimal;

public record ExpenseAnalysisGroupDto(
        String groupKey,
        String groupName,
        int claimCount,
        BigDecimal totalAmount,
        BigDecimal reimbursableAmount,
        Double percentageOfTotal
) {}

