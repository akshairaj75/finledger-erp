package com.backend.finledger.banking.dto;

import jakarta.validation.constraints.NotNull;

public record ReconciliationMatchRequest(
        @NotNull(message = "Statement line ID is required")
        Long statementLineId,

        @NotNull(message = "Journal line ID is required")
        Long journalLineId
) {}

