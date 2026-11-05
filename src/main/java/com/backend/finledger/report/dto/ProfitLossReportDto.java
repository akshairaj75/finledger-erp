package com.backend.finledger.report.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ProfitLossReportDto(
        LocalDate fromDate,
        LocalDate toDate,
        String comparePeriod,
        List<ReportLineDto> revenueLines,
        BigDecimal totalRevenue,
        BigDecimal totalRevenueCompare,
        List<ReportLineDto> cogsLines,
        BigDecimal totalCogs,
        BigDecimal totalCogsCompare,
        BigDecimal grossProfit,
        BigDecimal grossProfitCompare,
        List<ReportLineDto> expenseLines,
        BigDecimal totalExpenses,
        BigDecimal totalExpensesCompare,
        BigDecimal netProfit,
        BigDecimal netProfitCompare
) {}

