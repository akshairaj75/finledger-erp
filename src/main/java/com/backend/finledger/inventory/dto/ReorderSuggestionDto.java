package com.backend.finledger.inventory.dto;

import java.math.BigDecimal;

public record ReorderSuggestionDto(
    Long itemId,
    String sku,
    String name,
    String unit,
    BigDecimal totalOnHand,
    BigDecimal reorderLevel,
    BigDecimal reorderQty,
    BigDecimal suggestedOrderQty,
    BigDecimal purchasePrice,
    BigDecimal estimatedCost
) {}

