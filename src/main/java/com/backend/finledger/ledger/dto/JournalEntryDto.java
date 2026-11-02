package com.backend.finledger.ledger.dto;

import com.backend.finledger.ledger.JournalSourceType;
import com.backend.finledger.ledger.JournalStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record JournalEntryDto(
    Long id,
    Long companyId,
    String entryNo,
    LocalDate entryDate,
    Long fiscalPeriodId,
    String reference,
    String narration,
    JournalSourceType sourceType,
    Long sourceId,
    String currencyCode,
    BigDecimal exchangeRate,
    BigDecimal totalDebit,
    BigDecimal totalCredit,
    JournalStatus status,
    LocalDateTime postedAt,
    Long postedBy,
    Long reversalOfId,
    List<JournalLineDto> lines
) {}
