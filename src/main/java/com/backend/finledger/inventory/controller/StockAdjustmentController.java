package com.backend.finledger.inventory.controller;

import com.backend.finledger.common.PageResponse;
import com.backend.finledger.inventory.dto.StockAdjustmentDto;
import com.backend.finledger.inventory.dto.StockAdjustmentRequest;
import com.backend.finledger.inventory.service.StockAdjustmentService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/stock-adjustments")
public class StockAdjustmentController {

    private final StockAdjustmentService adjustmentService;

    public StockAdjustmentController(StockAdjustmentService adjustmentService) {
        this.adjustmentService = adjustmentService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('STOCK_ADJUST')")
    public ResponseEntity<PageResponse<StockAdjustmentDto>> listAdjustments(
            @PageableDefault(size = 20, sort = "adjustmentDate", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(adjustmentService.getAdjustments(pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('STOCK_ADJUST')")
    public ResponseEntity<StockAdjustmentDto> getAdjustment(@PathVariable Long id) {
        return ResponseEntity.ok(adjustmentService.getAdjustmentById(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('STOCK_ADJUST')")
    public ResponseEntity<StockAdjustmentDto> createAdjustment(@Valid @RequestBody StockAdjustmentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adjustmentService.createDraftAdjustment(request));
    }

    @PostMapping("/{id}/post")
    @PreAuthorize("hasAuthority('STOCK_ADJUST')")
    public ResponseEntity<StockAdjustmentDto> postAdjustment(@PathVariable Long id) {
        return ResponseEntity.ok(adjustmentService.postAdjustment(id));
    }

    @PostMapping("/{id}/void")
    @PreAuthorize("hasAuthority('STOCK_ADJUST')")
    public ResponseEntity<StockAdjustmentDto> voidAdjustment(
            @PathVariable Long id,
            @RequestParam(required = false) String reason) {
        String finalReason = (reason != null && !reason.isBlank()) ? reason : "Stock adjustment voided";
        return ResponseEntity.ok(adjustmentService.voidAdjustment(id, finalReason));
    }
}

