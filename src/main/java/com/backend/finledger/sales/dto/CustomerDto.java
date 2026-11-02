package com.backend.finledger.sales.dto;

import java.math.BigDecimal;

public record CustomerDto(
        Long id,
        Long companyId,
        String code,
        String name,
        String trn,
        String email,
        String phone,
        String billingAddress,
        String countryCode,
        String currencyCode,
        Short paymentTermsDays,
        BigDecimal creditLimit,
        Long arAccountId,
        Long accountManagerId,
        Long sourceLeadId,
        Boolean isActive
) {}

