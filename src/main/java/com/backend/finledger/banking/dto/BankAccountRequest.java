package com.backend.finledger.banking.dto;

import com.backend.finledger.banking.entity.AccountKind;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record BankAccountRequest(
        @NotBlank(message = "Account name is required")
        @Size(max = 100, message = "Name must not exceed 100 characters")
        String name,

        @Size(max = 100, message = "Bank name must not exceed 100 characters")
        String bankName,

        @Size(max = 40, message = "Account number must not exceed 40 characters")
        String accountNumber,

        @Size(max = 34, message = "IBAN must not exceed 34 characters")
        String iban,

        @Size(max = 11, message = "SWIFT code must not exceed 11 characters")
        String swiftCode,

        @NotBlank(message = "Currency code is required")
        @Size(min = 3, max = 3, message = "Currency code must be 3 characters")
        String currencyCode,

        @NotNull(message = "GL Account ID is required")
        Long glAccountId,

        AccountKind accountKind,

        Boolean isActive
) {}

