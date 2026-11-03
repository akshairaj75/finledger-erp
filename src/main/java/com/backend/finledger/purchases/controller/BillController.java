package com.backend.finledger.purchases.controller;

import com.backend.finledger.common.PageResponse;
import com.backend.finledger.purchases.dto.BillDto;
import com.backend.finledger.purchases.dto.BillRequest;
import com.backend.finledger.purchases.service.BillService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/bills")
public class BillController {

    private final BillService billService;

    public BillController(BillService billService) {
        this.billService = billService;
    }

    @PostMapping
    public ResponseEntity<BillDto> createBill(@Valid @RequestBody BillRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(billService.createBill(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<BillDto> updateBill(@PathVariable Long id, @Valid @RequestBody BillRequest request) {
        return ResponseEntity.ok(billService.updateBill(id, request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BillDto> getBill(@PathVariable Long id) {
        return ResponseEntity.ok(billService.getBillById(id));
    }

    @GetMapping
    public ResponseEntity<PageResponse<BillDto>> getBills(
            @RequestParam(required = false) Long vendorId,
            Pageable pageable) {
        return ResponseEntity.ok(billService.getBills(vendorId, pageable));
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<BillDto> approveBill(@PathVariable Long id) {
        return ResponseEntity.ok(billService.approveBill(id));
    }

    @PostMapping("/{id}/void")
    public ResponseEntity<BillDto> voidBill(
            @PathVariable Long id,
            @RequestParam(required = false) String reason) {
        return ResponseEntity.ok(billService.voidBill(id, reason));
    }
}

