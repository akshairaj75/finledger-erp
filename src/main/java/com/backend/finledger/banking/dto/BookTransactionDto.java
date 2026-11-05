package com.backend.finledger.banking.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record BookTransactionDto(
        Long journalLineId,
        Long journalEntryId,
        String entryNo,
        LocalDate entryDate,
        String reference,
        String description,
        BigDecimal debit,
        BigDecimal credit,
        BigDecimal netAmount,
        Boolean isReconciled
) {}

