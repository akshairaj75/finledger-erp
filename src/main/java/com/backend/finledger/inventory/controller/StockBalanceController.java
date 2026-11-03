package com.backend.finledger.inventory.controller;

import com.backend.finledger.inventory.dto.StockBalanceDto;
import com.backend.finledger.inventory.service.StockService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/stock-balances")
public class StockBalanceController {

    private final StockService stockService;

    public StockBalanceController(StockService stockService) {
        this.stockService = stockService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ITEM_VIEW')")
    public ResponseEntity<List<StockBalanceDto>> getStockBalances(
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) Long itemId) {

        if (itemId != null && warehouseId != null) {
            StockBalanceDto dto = stockService.getStockBalance(itemId, warehouseId);
            return ResponseEntity.ok(List.of(dto));
        } else if (itemId != null) {
            return ResponseEntity.ok(stockService.getStockBalancesByItem(itemId));
        } else if (warehouseId != null) {
            return ResponseEntity.ok(stockService.getStockBalancesByWarehouse(warehouseId));
        }

        return ResponseEntity.ok(List.of());
    }
}

