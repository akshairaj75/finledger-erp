package com.backend.finledger.sales.controller;

import com.backend.finledger.common.PageResponse;
import com.backend.finledger.common.VoidRequest;
import com.backend.finledger.sales.dto.ReceiptDto;
import com.backend.finledger.sales.dto.ReceiptRequest;
import com.backend.finledger.sales.service.ReceiptService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/receipts")
public class ReceiptController {

    private final ReceiptService receiptService;

    public ReceiptController(ReceiptService receiptService) {
        this.receiptService = receiptService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('RECEIPT_CREATE')")
    public ResponseEntity<ReceiptDto> createReceipt(@Valid @RequestBody ReceiptRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(receiptService.createReceipt(request));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('RECEIPT_VIEW')")
    public ResponseEntity<ReceiptDto> getReceipt(@PathVariable Long id) {
        return ResponseEntity.ok(receiptService.getReceiptById(id));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('RECEIPT_VIEW')")
    public ResponseEntity<PageResponse<ReceiptDto>> getReceipts(
            @RequestParam(required = false) Long customerId,
            Pageable pageable) {
        return ResponseEntity.ok(receiptService.getReceipts(customerId, pageable));
    }

    @PostMapping("/{id}/void")
    @PreAuthorize("hasAuthority('RECEIPT_VOID')")
    public ResponseEntity<ReceiptDto> voidReceipt(
            @PathVariable Long id,
            @RequestParam(required = false) String reason,
            @RequestBody(required = false) VoidRequest body) {
        return ResponseEntity.ok(receiptService.voidReceipt(id, VoidRequest.resolveReason(body, reason)));
    }
}

