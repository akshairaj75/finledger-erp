package com.backend.finledger.report.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record VatReturnReportDto(
        LocalDate fromDate,
        LocalDate toDate,
        BigDecimal standardRatedSalesAmount,
        BigDecimal standardRatedSalesVat,
        BigDecimal zeroRatedSalesAmount,
        BigDecimal exemptSalesAmount,
        BigDecimal standardRatedPurchasesAmount,
        BigDecimal standardRatedPurchasesVat,
        BigDecimal totalOutputVat,
        BigDecimal totalInputVat,
        BigDecimal netVatDue
) {}

