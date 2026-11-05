package com.backend.finledger.banking.dto;

import com.backend.finledger.banking.entity.MatchStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

public record BankStatementLineDto(
        Long id,
        Long bankStatementId,
        LocalDate txnDate,
        String description,
        String reference,
        BigDecimal amount,
        Long matchedJournalLineId,
        MatchStatus matchStatus
) {}

