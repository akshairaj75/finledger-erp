package com.backend.finledger.inventory.dto;

import com.backend.finledger.inventory.entity.AdjustmentReason;
import com.backend.finledger.inventory.entity.AdjustmentStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record StockAdjustmentDto(
    Long id,
    Long companyId,
    String adjustmentNo,
    LocalDate adjustmentDate,
    Long warehouseId,
    String warehouseName,
    AdjustmentReason reason,
    Long adjustmentAccountId,
    String adjustmentAccountName,
    String notes,
    BigDecimal totalValue,
    AdjustmentStatus status,
    Long journalEntryId,
    String journalEntryNo,
    List<StockAdjustmentLineDto> lines
) {}

