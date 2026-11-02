package com.backend.finledger.report;

import com.backend.finledger.ledger.dto.GeneralLedgerReportDto;
import com.backend.finledger.ledger.dto.TrialBalanceReportDto;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
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
}
