package com.backend.finledger.tax.dto;

import com.backend.finledger.tax.TaxType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record TaxRateRequest(
    @NotBlank(message = "Tax code is required")
    String code,

    @NotBlank(message = "Tax name is required")
    String name,

    @NotNull(message = "Rate is required")
    @DecimalMin(value = "0.0000", message = "Rate must be non-negative")
    BigDecimal rate,

    @NotNull(message = "Tax type is required")
    TaxType taxType,

    Long salesAccountId,
    Long purchaseAccountId,
    Boolean isActive
) {}

