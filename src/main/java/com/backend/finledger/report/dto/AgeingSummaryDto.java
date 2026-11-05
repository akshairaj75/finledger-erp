package com.backend.finledger.report.dto;

import java.math.BigDecimal;

public record AgeingSummaryDto(
        BigDecimal current,
        BigDecimal days1To30,
        BigDecimal days31To60,
        BigDecimal days61To90,
        BigDecimal over90,
        BigDecimal total
) {}

