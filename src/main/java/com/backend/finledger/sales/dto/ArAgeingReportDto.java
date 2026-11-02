package com.backend.finledger.sales.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ArAgeingReportDto(
        LocalDate asOfDate,
        List<ArAgeingCustomerDto> customers,
        BigDecimal totalCurrent,
        BigDecimal total1To30,
        BigDecimal total31To60,
        BigDecimal total61To90,
        BigDecimal totalOver90,
        BigDecimal grandTotal
) {}

