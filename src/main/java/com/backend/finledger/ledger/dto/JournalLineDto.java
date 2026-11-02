package com.backend.finledger.ledger.dto;

import com.backend.finledger.ledger.PartyType;
import java.math.BigDecimal;

public record JournalLineDto(
    Long id,
    Short lineNo,
    Long accountId,
    String accountCode,
    String accountName,
    Long costCenterId,
    String description,
    BigDecimal debit,
    BigDecimal credit,
    BigDecimal baseDebit,
    BigDecimal baseCredit,
    PartyType partyType,
    Long partyId,
    Boolean isReconciled
) {}
