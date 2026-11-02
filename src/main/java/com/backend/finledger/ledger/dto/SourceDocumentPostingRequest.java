package com.backend.finledger.ledger.dto;

import com.backend.finledger.ledger.JournalSourceType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record SourceDocumentPostingRequest(
    @NotNull(message = "Source type is required")
    JournalSourceType sourceType,

    @NotNull(message = "Source ID is required")
    Long sourceId,

    @NotNull(message = "Entry date is required")
    LocalDate entryDate,

    String reference,
    String narration,
    String currencyCode,
    BigDecimal exchangeRate,

    @NotEmpty(message = "Journal lines cannot be empty")
    @Valid
    List<JournalLineRequest> lines
) {}

