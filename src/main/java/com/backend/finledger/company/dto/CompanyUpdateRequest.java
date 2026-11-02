package com.backend.finledger.company.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CompanyUpdateRequest(
    @NotBlank(message = "Legal name is required")
    String legalName,

    String trn,

    @NotBlank(message = "Country code is required")
    String countryCode,

    @NotBlank(message = "Base currency is required")
    String baseCurrency,

    @NotNull(message = "Fiscal year start month is required")
    Byte fiscalYearStartMonth,

    String address,
    String email,
    String phone,
    String logoPath,
    Boolean isActive
) {}

