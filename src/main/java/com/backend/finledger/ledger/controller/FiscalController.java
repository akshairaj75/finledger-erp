package com.backend.finledger.ledger.controller;

import com.backend.finledger.ledger.dto.CreateFiscalYearRequest;
import com.backend.finledger.ledger.dto.FiscalPeriodDto;
import com.backend.finledger.ledger.dto.FiscalYearDto;
import com.backend.finledger.ledger.service.FiscalYearPeriodService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class FiscalController {

    private final FiscalYearPeriodService fiscalYearPeriodService;

    public FiscalController(FiscalYearPeriodService fiscalYearPeriodService) {
        this.fiscalYearPeriodService = fiscalYearPeriodService;
    }

    @GetMapping("/fiscal-years")
    @PreAuthorize("hasAnyAuthority('SETTINGS_VIEW','SETTINGS_EDIT','ACCOUNT_VIEW','JOURNAL_VIEW','JOURNAL_CREATE')")
    public ResponseEntity<List<FiscalYearDto>> getFiscalYears() {
        return ResponseEntity.ok(fiscalYearPeriodService.getFiscalYears());
    }

    @PostMapping("/fiscal-years")
    @PreAuthorize("hasAuthority('SETTINGS_EDIT')")
    public ResponseEntity<FiscalYearDto> createFiscalYear(@Valid @RequestBody CreateFiscalYearRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(fiscalYearPeriodService.createFiscalYear(request));
    }

    @PostMapping("/fiscal-periods/{id}/close")
    @PreAuthorize("hasAuthority('SETTINGS_EDIT')")
    public ResponseEntity<FiscalPeriodDto> closePeriod(@PathVariable Long id) {
        return ResponseEntity.ok(fiscalYearPeriodService.closePeriod(id));
    }

    @PostMapping("/fiscal-years/{id}/year-end-close")
    @PreAuthorize("hasAuthority('SETTINGS_EDIT')")
    public ResponseEntity<FiscalYearDto> closeFiscalYear(
            @PathVariable Long id,
            @RequestParam(required = false) Long retainedEarningsAccountId) {
        return ResponseEntity.ok(fiscalYearPeriodService.closeFiscalYear(id, retainedEarningsAccountId));
    }
}

