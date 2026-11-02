package com.backend.finledger.sales.dto;

import java.math.BigDecimal;

public record InvoiceLineDto(
        Long id,
        Short lineNo,
        Long itemId,
        String description,
        BigDecimal quantity,
        BigDecimal unitPrice,
        BigDecimal discountPct,
        Long taxRateId,
        BigDecimal taxAmount,
        BigDecimal lineTotal,
        Long accountId,
        Long warehouseId,
        BigDecimal unitCost,
        Long costCenterId
) {}

