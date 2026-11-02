package com.backend.finledger.ledger.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record GeneralLedgerLineDto(
    LocalDate date,
    String entryNo,
    String reference,
    String description,
    BigDecimal debit,
    BigDecimal credit,
    BigDecimal runningBalance
) {}
