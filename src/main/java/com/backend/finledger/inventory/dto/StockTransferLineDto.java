package com.backend.finledger.inventory.dto;

import java.math.BigDecimal;

public record StockTransferLineDto(
    Long id,
    Long itemId,
    String itemSku,
    String itemName,
    BigDecimal quantity,
    BigDecimal unitCost
) {}

