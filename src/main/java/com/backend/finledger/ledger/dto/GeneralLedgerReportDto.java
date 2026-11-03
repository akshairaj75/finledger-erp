package com.backend.finledger.ledger.dto;

import com.backend.finledger.ledger.entity.AccountType;
import com.backend.finledger.ledger.entity.NormalBalance;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record GeneralLedgerReportDto(
    Long accountId,
    String accountCode,
    String accountName,
    AccountType accountType,
    NormalBalance normalBalance,
    LocalDate fromDate,
    LocalDate toDate,
    BigDecimal openingBalance,
    BigDecimal totalDebit,
    BigDecimal totalCredit,
    BigDecimal closingBalance,
    List<GeneralLedgerLineDto> lines
) {}
