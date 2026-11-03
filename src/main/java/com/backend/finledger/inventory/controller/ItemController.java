package com.backend.finledger.inventory.controller;

import com.backend.finledger.common.PageResponse;
import com.backend.finledger.inventory.dto.*;
import com.backend.finledger.inventory.entity.ItemType;
import com.backend.finledger.inventory.service.InventoryReportService;
import com.backend.finledger.inventory.service.ItemService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/items")
public class ItemController {

    private final ItemService itemService;
    private final InventoryReportService reportService;

    public ItemController(ItemService itemService, InventoryReportService reportService) {
        this.itemService = itemService;
        this.reportService = reportService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ITEM_VIEW')")
    public ResponseEntity<PageResponse<ItemDto>> listItems(
            @RequestParam(required = false) ItemType itemType,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20, sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(itemService.searchItems(itemType, categoryId, active, search, pageable));
    }

    @GetMapping("/stock-summary")
    @PreAuthorize("hasAuthority('ITEM_VIEW')")
    public ResponseEntity<List<StockSummaryDto>> getStockSummary(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(reportService.getStockSummary(categoryId, warehouseId, status));
    }

    @GetMapping("/reorder-suggestions")
    @PreAuthorize("hasAuthority('ITEM_VIEW')")
    public ResponseEntity<List<ReorderSuggestionDto>> getReorderSuggestions() {
        return ResponseEntity.ok(reportService.getReorderSuggestions());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ITEM_VIEW')")
    public ResponseEntity<ItemDto> getItem(@PathVariable Long id) {
        return ResponseEntity.ok(itemService.getItemById(id));
    }

    @GetMapping("/{id}/movements")
    @PreAuthorize("hasAuthority('ITEM_VIEW')")
    public ResponseEntity<List<StockMovementDto>> getItemMovements(
            @PathVariable Long id,
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(reportService.getItemMovements(id, warehouseId, from, to));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ITEM_EDIT')")
    public ResponseEntity<ItemDto> createItem(@Valid @RequestBody ItemRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(itemService.createItem(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ITEM_EDIT')")
    public ResponseEntity<ItemDto> updateItem(
            @PathVariable Long id,
            @Valid @RequestBody ItemRequest request) {
        return ResponseEntity.ok(itemService.updateItem(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ITEM_EDIT')")
    public ResponseEntity<Void> deleteItem(@PathVariable Long id) {
        itemService.deleteItem(id);
        return ResponseEntity.noContent().build();
    }
}

