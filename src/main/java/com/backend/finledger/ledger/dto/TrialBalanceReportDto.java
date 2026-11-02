package com.backend.finledger.ledger.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record TrialBalanceReportDto(
    LocalDate asOfDate,
    BigDecimal totalDebit,
    BigDecimal totalCredit,
    Boolean isBalanced,
    List<TrialBalanceLineDto> lines
) {}
