package com.backend.finledger.banking.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record BankStatementDto(
        Long id,
        Long bankAccountId,
        LocalDate statementDate,
        BigDecimal openingBalance,
        BigDecimal closingBalance,
        String fileName,
        List<BankStatementLineDto> lines
) {}

