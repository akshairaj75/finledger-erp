package com.backend.finledger.ledger.dto;

import com.backend.finledger.ledger.entity.JournalSourceType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record CreateJournalEntryRequest(
    @NotNull(message = "Entry date is required")
    LocalDate entryDate,

    String reference,
    String narration,
    JournalSourceType sourceType,
    Long sourceId,
    String currencyCode,
    BigDecimal exchangeRate,

    @NotEmpty(message = "Journal lines cannot be empty")
    @Valid
    List<JournalLineRequest> lines,

    Boolean postImmediately
) {}
