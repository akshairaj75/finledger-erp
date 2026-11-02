package com.backend.finledger.ledger.dto;

import com.backend.finledger.ledger.AccountType;
import com.backend.finledger.ledger.NormalBalance;
import java.math.BigDecimal;

public record AccountDto(
    Long id,
    Long companyId,
    String code,
    String name,
    AccountType accountType,
    String subType,
    Long parentId,
    Boolean isGroup,
    NormalBalance normalBalance,
    String currencyCode,
    BigDecimal openingBalance,
    Boolean isSystem,
    Boolean isActive
) {}

