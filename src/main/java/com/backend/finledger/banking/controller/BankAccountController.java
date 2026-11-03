package com.backend.finledger.banking.controller;

import com.backend.finledger.banking.dto.BankAccountDto;
import com.backend.finledger.banking.service.BankAccountService;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<List<BankAccountDto>> listBankAccounts() {
        return ResponseEntity.ok(bankAccountService.getActiveBankAccounts());
    }

    @GetMapping("/{id}")
    public ResponseEntity<BankAccountDto> getBankAccount(@PathVariable Long id) {
        return ResponseEntity.ok(bankAccountService.getBankAccountById(id));
    }
}

