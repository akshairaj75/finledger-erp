package com.backend.finledger.report.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record StockMovementReportDto(
        Long itemId,
        String sku,
        String itemName,
        LocalDate fromDate,
        LocalDate toDate,
        BigDecimal openingQty,
        BigDecimal closingQty,
        List<StockMovementLineDto> movements
) {}

