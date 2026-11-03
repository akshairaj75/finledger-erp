package com.backend.finledger.purchases.dto;

import java.math.BigDecimal;

public record BillLineDto(
        Long id,
        Short lineNo,
        Long itemId,
        Long accountId,
        Long warehouseId,
        String description,
        BigDecimal quantity,
        BigDecimal unitPrice,
        Long taxRateId,
        BigDecimal taxAmount,
        BigDecimal lineTotal,
        Long costCenterId
) {}

