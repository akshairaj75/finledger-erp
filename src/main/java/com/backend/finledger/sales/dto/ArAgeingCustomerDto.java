package com.backend.finledger.sales.dto;

import java.math.BigDecimal;

public record ArAgeingCustomerDto(
        Long customerId,
        String customerCode,
        String customerName,
        BigDecimal current,
        BigDecimal days1To30,
        BigDecimal days31To60,
        BigDecimal days61To90,
        BigDecimal daysOver90,
        BigDecimal totalOutstanding
) {}

