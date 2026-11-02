package com.backend.finledger.currency;

import com.backend.finledger.currency.dto.CurrencyDto;
import com.backend.finledger.currency.dto.ExchangeRateDto;
import com.backend.finledger.currency.dto.ExchangeRateRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<List<CurrencyDto>> listCurrencies() {
        return ResponseEntity.ok(currencyService.getAllCurrencies());
    }

    @GetMapping("/exchange-rates")
    public ResponseEntity<List<ExchangeRateDto>> listExchangeRates() {
        return ResponseEntity.ok(currencyService.getExchangeRates());
    }

    @PostMapping("/exchange-rates")
    public ResponseEntity<ExchangeRateDto> setExchangeRate(@Valid @RequestBody ExchangeRateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(currencyService.setExchangeRate(request));
    }
}

