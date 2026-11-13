package com.backend.finledger.purchases.controller;

import com.backend.finledger.common.PageResponse;
import com.backend.finledger.common.VoidRequest;
import com.backend.finledger.purchases.dto.BillDto;
import com.backend.finledger.purchases.dto.BillRequest;
import com.backend.finledger.purchases.entity.BillStatus;
import com.backend.finledger.purchases.service.BillService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/bills")
public class BillController {

    private final BillService billService;

    public BillController(BillService billService) {
        this.billService = billService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('BILL_CREATE')")
    public ResponseEntity<BillDto> createBill(@Valid @RequestBody BillRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(billService.createBill(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('BILL_CREATE')")
    public ResponseEntity<BillDto> updateBill(@PathVariable Long id, @Valid @RequestBody BillRequest request) {
        return ResponseEntity.ok(billService.updateBill(id, request));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('BILL_VIEW')")
    public ResponseEntity<BillDto> getBill(@PathVariable Long id) {
        return ResponseEntity.ok(billService.getBillById(id));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('BILL_VIEW')")
    public ResponseEntity<PageResponse<BillDto>> getBills(
            @RequestParam(required = false) Long vendorId,
            @RequestParam(required = false) BillStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String search,
            Pageable pageable) {
        String effectiveQuery = (q != null && !q.isBlank()) ? q : search;
        return ResponseEntity.ok(billService.getBills(vendorId, status, from, to, effectiveQuery, pageable));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('BILL_APPROVE')")
    public ResponseEntity<BillDto> approveBill(@PathVariable Long id) {
        return ResponseEntity.ok(billService.approveBill(id));
    }

    @PostMapping("/{id}/void")
    @PreAuthorize("hasAuthority('BILL_VOID')")
    public ResponseEntity<BillDto> voidBill(
            @PathVariable Long id,
            @RequestParam(required = false) String reason,
            @RequestBody(required = false) VoidRequest body) {
        return ResponseEntity.ok(billService.voidBill(id, VoidRequest.resolveReason(body, reason)));
    }
}

