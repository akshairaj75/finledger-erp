package com.backend.finledger.company.dto;

public record CompanyDto(
    Long id,
    String code,
    String legalName,
    String trn,
    String countryCode,
    String baseCurrency,
    Byte fiscalYearStartMonth,
    String address,
    String email,
    String phone,
    String logoPath,
    Boolean isActive
) {}

