package com.backend.finledger.inventory.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record StockTransferLineRequest(
    @NotNull(message = "Item ID is required")
    Long itemId,

    @NotNull(message = "Quantity is required")
    @DecimalMin(value = "0.0001", message = "Transfer quantity must be greater than zero")
    BigDecimal quantity
) {}

