package com.backend.finledger.report.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record StockMovementLineDto(
        Long id,
        LocalDate date,
        String movementType,
        String reference,
        String warehouseName,
        BigDecimal qtyIn,
        BigDecimal qtyOut,
        BigDecimal runningBalance,
        BigDecimal unitCost
) {}

