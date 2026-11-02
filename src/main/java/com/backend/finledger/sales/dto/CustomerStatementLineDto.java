package com.backend.finledger.sales.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CustomerStatementLineDto(
        LocalDate date,
        String docType,
        String referenceNo,
        String description,
        BigDecimal debit,
        BigDecimal credit,
        BigDecimal runningBalance
) {}

