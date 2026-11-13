package com.backend.finledger.currency.controller;

import com.backend.finledger.currency.dto.CurrencyDto;
import com.backend.finledger.currency.dto.ExchangeRateDto;
import com.backend.finledger.currency.dto.ExchangeRateRequest;
import com.backend.finledger.currency.service.CurrencyService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/currencies")
public class CurrencyController {

    private final CurrencyService currencyService;

    public CurrencyController(CurrencyService currencyService) {
        this.currencyService = currencyService;
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('SETTINGS_VIEW','SETTINGS_EDIT','CURRENCY_VIEW','CURRENCY_EDIT','INVOICE_CREATE','BILL_CREATE','QUOTATION_CREATE','JOURNAL_CREATE','EXPENSE_CREATE')")
    public ResponseEntity<List<CurrencyDto>> getAll() {
        return ResponseEntity.ok(currencyService.getAllCurrencies());
    }

    @GetMapping("/exchange-rates")
    @PreAuthorize("hasAnyAuthority('SETTINGS_VIEW','SETTINGS_EDIT','CURRENCY_VIEW','CURRENCY_EDIT','INVOICE_CREATE','BILL_CREATE','QUOTATION_CREATE','JOURNAL_CREATE','EXPENSE_CREATE')")
    public ResponseEntity<List<ExchangeRateDto>> getExchangeRates() {
        return ResponseEntity.ok(currencyService.getExchangeRates());
    }

    @PostMapping("/exchange-rates")
    @PreAuthorize("hasAuthority('SETTINGS_EDIT') or hasAuthority('CURRENCY_EDIT')")
    public ResponseEntity<ExchangeRateDto> setExchangeRate(@Valid @RequestBody ExchangeRateRequest request) {
        return ResponseEntity.ok(currencyService.setExchangeRate(request));
    }
}

