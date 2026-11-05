package com.backend.finledger.report.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record BalanceSheetReportDto(
        LocalDate asOfDate,
        String comparePeriod,
        List<ReportLineDto> currentAssets,
        BigDecimal totalCurrentAssets,
        List<ReportLineDto> nonCurrentAssets,
        BigDecimal totalNonCurrentAssets,
        BigDecimal totalAssets,
        List<ReportLineDto> currentLiabilities,
        BigDecimal totalCurrentLiabilities,
        List<ReportLineDto> nonCurrentLiabilities,
        BigDecimal totalNonCurrentLiabilities,
        BigDecimal totalLiabilities,
        List<ReportLineDto> equityLines,
        BigDecimal retainedEarnings,
        BigDecimal netIncomeYearToDate,
        BigDecimal totalEquity,
        BigDecimal totalLiabilitiesAndEquity,
        boolean balanced
) {}

