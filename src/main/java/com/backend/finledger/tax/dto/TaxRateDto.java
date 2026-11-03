package com.backend.finledger.tax.dto;

import com.backend.finledger.tax.entity.TaxType;
import java.math.BigDecimal;

public record TaxRateDto(
    Long id,
    Long companyId,
    String code,
    String name,
    BigDecimal rate,
    TaxType taxType,
    Long salesAccountId,
    Long purchaseAccountId,
    Boolean isActive
) {}

