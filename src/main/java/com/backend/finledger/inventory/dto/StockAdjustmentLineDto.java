package com.backend.finledger.inventory.dto;

import java.math.BigDecimal;

public record StockAdjustmentLineDto(
    Long id,
    Long itemId,
    String itemSku,
    String itemName,
    BigDecimal systemQty,
    BigDecimal countedQty,
    BigDecimal differenceQty,
    BigDecimal unitCost,
    BigDecimal valueImpact
) {}

