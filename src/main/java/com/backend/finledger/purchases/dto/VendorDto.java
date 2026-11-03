package com.backend.finledger.purchases.dto;

public record VendorDto(
        Long id,
        Long companyId,
        String code,
        String name,
        String trn,
        String email,
        String phone,
        String address,
        String currencyCode,
        Short paymentTermsDays,
        String bankIban,
        Long apAccountId,
        Boolean isActive
) {}

