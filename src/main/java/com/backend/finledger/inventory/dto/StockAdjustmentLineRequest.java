package com.backend.finledger.inventory.dto;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record StockAdjustmentLineRequest(
    @NotNull(message = "Item ID is required")
    Long itemId,

    @NotNull(message = "Counted quantity is required")
    BigDecimal countedQty,

    BigDecimal unitCost
) {}

