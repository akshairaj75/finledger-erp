package com.backend.finledger.tax;

import com.backend.finledger.tax.dto.TaxRateDto;
import com.backend.finledger.tax.dto.TaxRateRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<List<TaxRateDto>> listTaxRates() {
        return ResponseEntity.ok(taxRateService.getTaxRates());
    }

    @GetMapping("/{id}")
    public ResponseEntity<TaxRateDto> getTaxRate(@PathVariable Long id) {
        return ResponseEntity.ok(taxRateService.getTaxRateById(id));
    }

    @PostMapping
    public ResponseEntity<TaxRateDto> createTaxRate(@Valid @RequestBody TaxRateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(taxRateService.createTaxRate(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TaxRateDto> updateTaxRate(@PathVariable Long id, @Valid @RequestBody TaxRateRequest request) {
        return ResponseEntity.ok(taxRateService.updateTaxRate(id, request));
    }
}

