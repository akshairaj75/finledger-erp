package com.backend.finledger.inventory.controller;

import com.backend.finledger.common.PageResponse;
import com.backend.finledger.inventory.dto.StockTransferDto;
import com.backend.finledger.inventory.dto.StockTransferRequest;
import com.backend.finledger.inventory.service.StockTransferService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/stock-transfers")
public class StockTransferController {

    private final StockTransferService transferService;

    public StockTransferController(StockTransferService transferService) {
        this.transferService = transferService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('STOCK_TRANSFER')")
    public ResponseEntity<PageResponse<StockTransferDto>> listTransfers(
            @PageableDefault(size = 20, sort = "transferDate", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(transferService.getTransfers(pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('STOCK_TRANSFER')")
    public ResponseEntity<StockTransferDto> getTransfer(@PathVariable Long id) {
        return ResponseEntity.ok(transferService.getTransferById(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('STOCK_TRANSFER')")
    public ResponseEntity<StockTransferDto> createTransfer(@Valid @RequestBody StockTransferRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(transferService.createDraftTransfer(request));
    }

    @PostMapping("/{id}/dispatch")
    @PreAuthorize("hasAuthority('STOCK_TRANSFER')")
    public ResponseEntity<StockTransferDto> dispatchTransfer(@PathVariable Long id) {
        return ResponseEntity.ok(transferService.dispatchTransfer(id));
    }

    @PostMapping("/{id}/receive")
    @PreAuthorize("hasAuthority('STOCK_TRANSFER')")
    public ResponseEntity<StockTransferDto> receiveTransfer(@PathVariable Long id) {
        return ResponseEntity.ok(transferService.receiveTransfer(id));
    }

    @PostMapping("/{id}/void")
    @PreAuthorize("hasAuthority('STOCK_TRANSFER')")
    public ResponseEntity<StockTransferDto> voidTransfer(
            @PathVariable Long id,
            @RequestParam(required = false) String reason) {
        String finalReason = (reason != null && !reason.isBlank()) ? reason : "Stock transfer voided";
        return ResponseEntity.ok(transferService.voidTransfer(id, finalReason));
    }
}

