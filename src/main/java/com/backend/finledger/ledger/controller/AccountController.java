package com.backend.finledger.ledger.controller;

import com.backend.finledger.common.PageResponse;
import com.backend.finledger.ledger.dto.AccountDto;
import com.backend.finledger.ledger.dto.AccountNodeDto;
import com.backend.finledger.ledger.dto.AccountRequest;
import com.backend.finledger.ledger.entity.AccountType;
import com.backend.finledger.ledger.service.AccountService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ACCOUNT_VIEW','JOURNAL_VIEW','JOURNAL_CREATE','INVOICE_CREATE','BILL_CREATE','SETTINGS_VIEW','SETTINGS_EDIT')")
    public ResponseEntity<PageResponse<AccountDto>> listAccounts(
            @RequestParam(required = false) AccountType type,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) String q,
            @PageableDefault(size = 50) Pageable pageable) {
        return ResponseEntity.ok(accountService.getAccounts(type, active, q, pageable));
    }

    @GetMapping("/tree")
    @PreAuthorize("hasAnyAuthority('ACCOUNT_VIEW','JOURNAL_VIEW','JOURNAL_CREATE','INVOICE_CREATE','BILL_CREATE','SETTINGS_VIEW','SETTINGS_EDIT')")
    public ResponseEntity<List<AccountNodeDto>> getTree() {
        return ResponseEntity.ok(accountService.getAccountsTree());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ACCOUNT_VIEW','JOURNAL_VIEW','JOURNAL_CREATE','INVOICE_CREATE','BILL_CREATE','SETTINGS_VIEW','SETTINGS_EDIT')")
    public ResponseEntity<AccountDto> getAccount(@PathVariable Long id) {
        return ResponseEntity.ok(accountService.getAccountById(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ACCOUNT_CREATE') or hasAuthority('SETTINGS_EDIT')")
    public ResponseEntity<AccountDto> createAccount(@Valid @RequestBody AccountRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(accountService.createAccount(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ACCOUNT_EDIT') or hasAuthority('SETTINGS_EDIT')")
    public ResponseEntity<AccountDto> updateAccount(@PathVariable Long id, @Valid @RequestBody AccountRequest request) {
        return ResponseEntity.ok(accountService.updateAccount(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ACCOUNT_DELETE') or hasAuthority('SETTINGS_EDIT')")
    public ResponseEntity<Void> deleteAccount(@PathVariable Long id) {
        accountService.deleteAccount(id);
        return ResponseEntity.noContent().build();
    }
}

