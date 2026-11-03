package com.backend.finledger.inventory.dto;

import com.backend.finledger.inventory.entity.MovementType;
import com.backend.finledger.inventory.entity.StockSourceType;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record StockMovementDto(
    Long id,
    Long companyId,
    Long itemId,
    String itemSku,
    String itemName,
    Long warehouseId,
    String warehouseCode,
    String warehouseName,
    LocalDate movementDate,
    MovementType movementType,
    BigDecimal quantity,
    BigDecimal unitCost,
    BigDecimal totalCost,
    BigDecimal balanceQty,
    StockSourceType sourceType,
    Long sourceId,
    Long sourceLineId,
    Long journalEntryId,
    Instant createdAt
) {}

