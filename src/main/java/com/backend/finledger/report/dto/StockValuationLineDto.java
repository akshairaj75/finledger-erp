package com.backend.finledger.report.dto;

import java.math.BigDecimal;

public record StockValuationLineDto(
        Long itemId,
        String sku,
        String itemName,
        String categoryName,
        String warehouseName,
        BigDecimal quantityOnHand,
        BigDecimal unitCost,
        BigDecimal totalValue
) {}

