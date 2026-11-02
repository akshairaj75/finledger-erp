package com.backend.finledger.ledger.dto;

import com.backend.finledger.ledger.AccountType;
import com.backend.finledger.ledger.NormalBalance;
import java.math.BigDecimal;

public record TrialBalanceLineDto(
    Long accountId,
    String accountCode,
    String accountName,
    AccountType accountType,
    NormalBalance normalBalance,
    BigDecimal debitBalance,
    BigDecimal creditBalance
) {}
