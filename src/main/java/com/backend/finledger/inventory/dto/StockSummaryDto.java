package com.backend.finledger.inventory.dto;

import com.backend.finledger.inventory.entity.ItemType;

import java.math.BigDecimal;
import java.util.Map;

public record StockSummaryDto(
    Long itemId,
    String sku,
    String name,
    Long categoryId,
    String categoryName,
    ItemType itemType,
    String unit,
    BigDecimal totalOnHand,
    BigDecimal totalReserved,
    BigDecimal totalAvailable,
    BigDecimal avgCost,
    BigDecimal totalStockValue,
    BigDecimal reorderLevel,
    BigDecimal reorderQty,
    String status, // "IN_STOCK", "LOW_STOCK", "OUT_OF_STOCK"
    Map<String, BigDecimal> warehouseQuantities
) {}

