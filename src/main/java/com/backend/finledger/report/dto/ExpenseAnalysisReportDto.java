package com.backend.finledger.report.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ExpenseAnalysisReportDto(
        LocalDate fromDate,
        LocalDate toDate,
        String groupBy,
        List<ExpenseAnalysisGroupDto> groups,
        BigDecimal grandTotal
) {}

