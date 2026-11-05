package com.backend.finledger.report.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RecentTransactionDto(
        Long id,
        LocalDate date,
        String type,
        String reference,
        String description,
        BigDecimal amount,
        String status
) {}

