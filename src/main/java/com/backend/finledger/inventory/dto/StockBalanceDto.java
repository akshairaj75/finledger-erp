package com.backend.finledger.inventory.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record StockBalanceDto(
    Long id,
    Long itemId,
    String itemSku,
    String itemName,
    Long warehouseId,
    String warehouseCode,
    String warehouseName,
    BigDecimal qtyOnHand,
    BigDecimal qtyReserved,
    BigDecimal qtyAvailable,
    BigDecimal avgCost,
    BigDecimal stockValue,
    Instant updatedAt
) {}

