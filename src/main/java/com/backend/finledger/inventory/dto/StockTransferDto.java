package com.backend.finledger.inventory.dto;

import com.backend.finledger.inventory.entity.TransferStatus;

import java.time.LocalDate;
import java.util.List;

public record StockTransferDto(
    Long id,
    Long companyId,
    String transferNo,
    LocalDate transferDate,
    Long fromWarehouseId,
    String fromWarehouseName,
    Long toWarehouseId,
    String toWarehouseName,
    TransferStatus status,
    String notes,
    List<StockTransferLineDto> lines
) {}

