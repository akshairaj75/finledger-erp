package com.backend.finledger.purchases.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ApAgeingReportDto(
        LocalDate asOfDate,
        List<ApAgeingVendorDto> vendors,
        BigDecimal totalCurrent,
        BigDecimal total1To30,
        BigDecimal total31To60,
        BigDecimal total61To90,
        BigDecimal totalOver90,
        BigDecimal grandTotal
) {}

