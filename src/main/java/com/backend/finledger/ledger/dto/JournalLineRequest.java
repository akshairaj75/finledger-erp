package com.backend.finledger.ledger.dto;

import com.backend.finledger.ledger.entity.PartyType;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record JournalLineRequest(
    @NotNull(message = "Account ID is required")
    Long accountId,

    Long costCenterId,
    String description,
    BigDecimal debit,
    BigDecimal credit,
    PartyType partyType,
    Long partyId
) {}
