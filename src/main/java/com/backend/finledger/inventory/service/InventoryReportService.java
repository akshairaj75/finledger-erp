package com.backend.finledger.inventory.service;

import com.backend.finledger.inventory.dto.ReorderSuggestionDto;
import com.backend.finledger.inventory.dto.StockMovementDto;
import com.backend.finledger.inventory.dto.StockSummaryDto;

import java.time.LocalDate;
import java.util.List;

public interface InventoryReportService {
    List<StockSummaryDto> getStockSummary(Long categoryId, Long warehouseId, String status);
    List<StockMovementDto> getItemMovements(Long itemId, Long warehouseId, LocalDate from, LocalDate to);
    List<ReorderSuggestionDto> getReorderSuggestions();
}

