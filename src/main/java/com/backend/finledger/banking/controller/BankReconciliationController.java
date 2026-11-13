package com.backend.finledger.banking.controller;

import com.backend.finledger.banking.dto.*;
import com.backend.finledger.banking.service.BankReconciliationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/bank-reconciliations")
public class BankReconciliationController {

    private final BankReconciliationService reconciliationService;

    public BankReconciliationController(BankReconciliationService reconciliationService) {
        this.reconciliationService = reconciliationService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('BANK_RECONCILE')")
    public ResponseEntity<BankReconciliationDto> startReconciliation(@Valid @RequestBody BankReconciliationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(reconciliationService.startReconciliation(request));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('BANK_RECONCILE')")
    public ResponseEntity<BankReconciliationDto> getReconciliation(@PathVariable Long id) {
        return ResponseEntity.ok(reconciliationService.getReconciliationById(id));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('BANK_RECONCILE')")
    public ResponseEntity<List<BankReconciliationDto>> getReconciliationsByBankAccount(
            @RequestParam Long bankAccountId) {
        return ResponseEntity.ok(reconciliationService.getReconciliationsByBankAccountId(bankAccountId));
    }

    @PostMapping("/{id}/match")
    @PreAuthorize("hasAuthority('BANK_RECONCILE')")
    public ResponseEntity<BankReconciliationDto> match(
            @PathVariable Long id,
            @Valid @RequestBody ReconciliationMatchRequest request) {
        return ResponseEntity.ok(reconciliationService.match(id, request));
    }

    @PostMapping("/{id}/unmatch/{statementLineId}")
    @PreAuthorize("hasAuthority('BANK_RECONCILE')")
    public ResponseEntity<BankReconciliationDto> unmatch(
            @PathVariable Long id,
            @PathVariable Long statementLineId) {
        return ResponseEntity.ok(reconciliationService.unmatch(id, statementLineId));
    }

    @PostMapping("/{id}/auto-match")
    @PreAuthorize("hasAuthority('BANK_RECONCILE')")
    public ResponseEntity<AutoMatchResultDto> autoMatch(@PathVariable Long id) {
        return ResponseEntity.ok(reconciliationService.autoMatch(id));
    }

    @PostMapping("/{id}/bank-charge")
    @PreAuthorize("hasAuthority('BANK_RECONCILE')")
    public ResponseEntity<BankReconciliationDto> createBankCharge(
            @PathVariable Long id,
            @Valid @RequestBody BankChargeRequest request) {
        return ResponseEntity.ok(reconciliationService.createBankCharge(id, request));
    }

    @PostMapping("/{id}/complete")
    @PreAuthorize("hasAuthority('BANK_RECONCILE')")
    public ResponseEntity<BankReconciliationDto> complete(@PathVariable Long id) {
        return ResponseEntity.ok(reconciliationService.completeReconciliation(id));
    }
}

