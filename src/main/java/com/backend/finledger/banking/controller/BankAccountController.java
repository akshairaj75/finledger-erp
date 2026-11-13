package com.backend.finledger.banking.controller;

import com.backend.finledger.banking.dto.BankAccountDto;
import com.backend.finledger.banking.dto.BankAccountRequest;
import com.backend.finledger.banking.service.BankAccountService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/bank-accounts")
public class BankAccountController {

    private final BankAccountService bankAccountService;

    public BankAccountController(BankAccountService bankAccountService) {
        this.bankAccountService = bankAccountService;
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('BANK_RECONCILE','COMPANY_VIEW','SETTINGS_VIEW','SETTINGS_EDIT','RECEIPT_CREATE','RECEIPT_VIEW','PAYMENT_CREATE','PAYMENT_VIEW','EXPENSE_CREATE','EXPENSE_VIEW','EXPENSE_REIMBURSE')")
    public ResponseEntity<List<BankAccountDto>> listBankAccounts(
            @RequestParam(required = false, defaultValue = "true") Boolean activeOnly) {
        if (Boolean.TRUE.equals(activeOnly)) {
            return ResponseEntity.ok(bankAccountService.getActiveBankAccounts());
        } else {
            return ResponseEntity.ok(bankAccountService.getAllBankAccounts());
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('BANK_RECONCILE','COMPANY_VIEW','SETTINGS_VIEW','SETTINGS_EDIT','RECEIPT_CREATE','RECEIPT_VIEW','PAYMENT_CREATE','PAYMENT_VIEW','EXPENSE_CREATE','EXPENSE_VIEW','EXPENSE_REIMBURSE')")
    public ResponseEntity<BankAccountDto> getBankAccount(@PathVariable Long id) {
        return ResponseEntity.ok(bankAccountService.getBankAccountById(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('BANK_RECONCILE') or hasAuthority('SETTINGS_EDIT')")
    public ResponseEntity<BankAccountDto> createBankAccount(@Valid @RequestBody BankAccountRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(bankAccountService.createBankAccount(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('BANK_RECONCILE') or hasAuthority('SETTINGS_EDIT')")
    public ResponseEntity<BankAccountDto> updateBankAccount(
            @PathVariable Long id,
            @Valid @RequestBody BankAccountRequest request) {
        return ResponseEntity.ok(bankAccountService.updateBankAccount(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('BANK_RECONCILE') or hasAuthority('SETTINGS_EDIT')")
    public ResponseEntity<Void> deleteBankAccount(@PathVariable Long id) {
        bankAccountService.deleteBankAccount(id);
        return ResponseEntity.noContent().build();
    }
}
