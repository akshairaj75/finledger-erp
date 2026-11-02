package com.backend.finledger.sales.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record CustomerStatementDto(
        Long customerId,
        String customerCode,
        String customerName,
        String currencyCode,
        LocalDate fromDate,
        LocalDate toDate,
        BigDecimal openingBalance,
        BigDecimal closingBalance,
        List<CustomerStatementLineDto> lines
) {}

