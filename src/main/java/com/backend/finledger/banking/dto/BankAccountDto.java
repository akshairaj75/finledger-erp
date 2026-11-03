package com.backend.finledger.banking.dto;

import com.backend.finledger.banking.entity.AccountKind;

public record BankAccountDto(
        Long id,
        Long companyId,
        String name,
        String bankName,
        String accountNumber,
        String iban,
        String swiftCode,
        String currencyCode,
        Long glAccountId,
        AccountKind accountKind,
        Boolean isActive
) {}

