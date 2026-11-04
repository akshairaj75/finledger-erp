package com.backend.finledger.crm.dto;

import java.math.BigDecimal;

public record QuotationLineDto(
    Long id,
    Short lineNo,
    Long itemId,
    String itemSku,
    String itemName,
    String description,
    BigDecimal quantity,
    BigDecimal unitPrice,
    BigDecimal discountPct,
    Long taxRateId,
    BigDecimal taxRatePct,
    BigDecimal lineTotal
) {}

