package com.backend.finledger.ledger.dto;

import com.backend.finledger.ledger.entity.AccountType;
import com.backend.finledger.ledger.entity.NormalBalance;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record AccountRequest(
    @NotBlank(message = "Account code is required")
    String code,

    @NotBlank(message = "Account name is required")
    String name,

    @NotNull(message = "Account type is required")
    AccountType accountType,

    String subType,
    Long parentId,
    Boolean isGroup,

    @NotNull(message = "Normal balance (DEBIT/CREDIT) is required")
    NormalBalance normalBalance,

    String currencyCode,
    BigDecimal openingBalance,
    Boolean isActive
) {}

