package com.backend.finledger.banking.dto;

import com.backend.finledger.banking.entity.ReconciliationStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record BankReconciliationDto(
        Long id,
        Long bankAccountId,
        String bankAccountName,
        Long glAccountId,
        LocalDate statementDate,
        BigDecimal statementBalance,
        BigDecimal bookBalance,
        BigDecimal difference,
        ReconciliationStatus status,
        Instant completedAt,
        List<BankStatementLineDto> statementLines,
        List<BookTransactionDto> bookTransactions,
        int matchedCount,
        int unmatchedStatementCount,
        int unmatchedBookCount
) {}

