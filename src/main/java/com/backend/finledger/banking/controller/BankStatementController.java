package com.backend.finledger.banking.controller;

import com.backend.finledger.banking.dto.BankStatementDto;
import com.backend.finledger.banking.service.BankStatementService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/bank-statements")
public class BankStatementController {

    private final BankStatementService bankStatementService;

    public BankStatementController(BankStatementService bankStatementService) {
        this.bankStatementService = bankStatementService;
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('BANK_RECONCILE') or isAuthenticated()")
    public ResponseEntity<BankStatementDto> importStatement(
            @RequestParam("file") MultipartFile file,
            @RequestParam("bankAccountId") Long bankAccountId,
            @RequestParam(value = "statementDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate statementDate,
            @RequestParam(value = "openingBalance", required = false) BigDecimal openingBalance,
            @RequestParam(value = "closingBalance", required = false) BigDecimal closingBalance) {
        BankStatementDto statement = bankStatementService.importStatement(
                file, bankAccountId, statementDate, openingBalance, closingBalance);
        return ResponseEntity.status(HttpStatus.CREATED).body(statement);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('BANK_RECONCILE') or isAuthenticated()")
    public ResponseEntity<List<BankStatementDto>> getStatements(@RequestParam Long bankAccountId) {
        return ResponseEntity.ok(bankStatementService.getStatementsByBankAccountId(bankAccountId));
    }
}

