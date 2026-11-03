package com.backend.finledger.report.controller;

import com.backend.finledger.ledger.dto.GeneralLedgerReportDto;
import com.backend.finledger.ledger.dto.TrialBalanceReportDto;
import com.backend.finledger.purchases.dto.ApAgeingReportDto;
import com.backend.finledger.purchases.service.ApAgeingService;
import com.backend.finledger.report.service.ReportService;
import com.backend.finledger.sales.dto.ArAgeingReportDto;
import com.backend.finledger.sales.service.ArAgeingService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/reports")
public class ReportController {

    private final ReportService reportService;
    private final ArAgeingService arAgeingService;
    private final ApAgeingService apAgeingService;

    public ReportController(ReportService reportService, ArAgeingService arAgeingService, ApAgeingService apAgeingService) {
        this.reportService = reportService;
        this.arAgeingService = arAgeingService;
        this.apAgeingService = apAgeingService;
    }

    @GetMapping("/general-ledger")
    public ResponseEntity<GeneralLedgerReportDto> getGeneralLedgerReport(
            @RequestParam Long accountId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(reportService.getGeneralLedgerReport(accountId, from, to));
    }

    @GetMapping("/trial-balance")
    public ResponseEntity<TrialBalanceReportDto> getTrialBalanceReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOf) {
        return ResponseEntity.ok(reportService.getTrialBalanceReport(asOf));
    }

    @GetMapping("/ar-ageing")
    public ResponseEntity<ArAgeingReportDto> getArAgeingReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOf) {
        return ResponseEntity.ok(arAgeingService.getArAgeingReport(asOf));
    }

    @GetMapping("/ap-ageing")
    public ResponseEntity<ApAgeingReportDto> getApAgeingReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOf) {
        return ResponseEntity.ok(apAgeingService.getApAgeingReport(asOf));
    }
}

