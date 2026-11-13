package com.backend.finledger.tax.controller;

import com.backend.finledger.tax.dto.TaxRateDto;
import com.backend.finledger.tax.dto.TaxRateRequest;
import com.backend.finledger.tax.service.TaxRateService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tax-rates")
public class TaxRateController {

    private final TaxRateService taxRateService;

    public TaxRateController(TaxRateService taxRateService) {
        this.taxRateService = taxRateService;
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('SETTINGS_VIEW','SETTINGS_EDIT','TAX_RATE_VIEW','TAX_RATE_EDIT','INVOICE_CREATE','BILL_CREATE','QUOTATION_CREATE','EXPENSE_CREATE','EXPENSE_VIEW')")
    public ResponseEntity<List<TaxRateDto>> listTaxRates() {
        return ResponseEntity.ok(taxRateService.getTaxRates());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('SETTINGS_VIEW','SETTINGS_EDIT','TAX_RATE_VIEW','TAX_RATE_EDIT','INVOICE_CREATE','BILL_CREATE','QUOTATION_CREATE','EXPENSE_CREATE','EXPENSE_VIEW')")
    public ResponseEntity<TaxRateDto> getTaxRate(@PathVariable Long id) {
        return ResponseEntity.ok(taxRateService.getTaxRateById(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('SETTINGS_EDIT') or hasAuthority('TAX_RATE_EDIT')")
    public ResponseEntity<TaxRateDto> createTaxRate(@Valid @RequestBody TaxRateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(taxRateService.createTaxRate(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('SETTINGS_EDIT') or hasAuthority('TAX_RATE_EDIT')")
    public ResponseEntity<TaxRateDto> updateTaxRate(@PathVariable Long id, @Valid @RequestBody TaxRateRequest request) {
        return ResponseEntity.ok(taxRateService.updateTaxRate(id, request));
    }
}

