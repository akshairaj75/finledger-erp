package com.backend.finledger.purchases.dto;

import java.math.BigDecimal;

public record ApAgeingVendorDto(
        Long vendorId,
        String vendorCode,
        String vendorName,
        BigDecimal current,
        BigDecimal days1To30,
        BigDecimal days31To60,
        BigDecimal days61To90,
        BigDecimal daysOver90,
        BigDecimal totalOutstanding
) {}

