package com.backend.finledger.report.controller;

import com.backend.finledger.ledger.dto.GeneralLedgerReportDto;
import com.backend.finledger.ledger.dto.TrialBalanceReportDto;
import com.backend.finledger.purchases.dto.ApAgeingReportDto;
import com.backend.finledger.purchases.service.ApAgeingService;
import com.backend.finledger.report.dto.*;
import com.backend.finledger.report.service.ReportService;
import com.backend.finledger.sales.dto.ArAgeingReportDto;
import com.backend.finledger.sales.service.ArAgeingService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

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
    @PreAuthorize("hasAuthority('REPORT_VIEW') or hasAuthority('LEDGER_VIEW')")
    public ResponseEntity<GeneralLedgerReportDto> getGeneralLedgerReport(
            @RequestParam Long accountId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(reportService.getGeneralLedgerReport(accountId, from, to));
    }

    @GetMapping("/trial-balance")
    @PreAuthorize("hasAuthority('REPORT_VIEW') or hasAuthority('LEDGER_VIEW')")
    public ResponseEntity<TrialBalanceReportDto> getTrialBalanceReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOf) {
        return ResponseEntity.ok(reportService.getTrialBalanceReport(asOf));
    }

    @GetMapping("/ar-ageing")
    @PreAuthorize("hasAuthority('REPORT_VIEW') or hasAuthority('INVOICE_VIEW')")
    public ResponseEntity<ArAgeingReportDto> getArAgeingReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOf) {
        return ResponseEntity.ok(arAgeingService.getArAgeingReport(asOf));
    }

    @GetMapping("/ap-ageing")
    @PreAuthorize("hasAuthority('REPORT_VIEW') or hasAuthority('BILL_VIEW')")
    public ResponseEntity<ApAgeingReportDto> getApAgeingReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOf) {
        return ResponseEntity.ok(apAgeingService.getApAgeingReport(asOf));
    }

    @GetMapping("/profit-loss")
    @PreAuthorize("hasAuthority('REPORT_VIEW')")
    public ResponseEntity<ProfitLossReportDto> getProfitLossReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String compare,
            @RequestParam(required = false) Long costCenterId) {
        return ResponseEntity.ok(reportService.getProfitLossReport(from, to, compare, costCenterId));
    }

    @GetMapping("/balance-sheet")
    @PreAuthorize("hasAuthority('REPORT_VIEW')")
    public ResponseEntity<BalanceSheetReportDto> getBalanceSheetReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOf,
            @RequestParam(required = false) String compare) {
        return ResponseEntity.ok(reportService.getBalanceSheetReport(asOf, compare));
    }

    @GetMapping("/vat-return")
    @PreAuthorize("hasAuthority('REPORT_VIEW')")
    public ResponseEntity<VatReturnReportDto> getVatReturnReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(reportService.getVatReturnReport(from, to));
    }

    @GetMapping("/stock-valuation")
    @PreAuthorize("hasAuthority('REPORT_VIEW') or hasAuthority('ITEM_VIEW')")
    public ResponseEntity<StockValuationReportDto> getStockValuationReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOf,
            @RequestParam(required = false) Long warehouseId) {
        return ResponseEntity.ok(reportService.getStockValuationReport(asOf, warehouseId));
    }

    @GetMapping("/stock-movement")
    @PreAuthorize("hasAuthority('REPORT_VIEW') or hasAuthority('ITEM_VIEW')")
    public ResponseEntity<StockMovementReportDto> getStockMovementReport(
            @RequestParam Long itemId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(reportService.getStockMovementReport(itemId, from, to));
    }

    @GetMapping("/pipeline-forecast")
    @PreAuthorize("hasAuthority('REPORT_VIEW') or hasAuthority('CRM_VIEW_ALL')")
    public ResponseEntity<PipelineForecastReportDto> getPipelineForecastReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Long ownerId) {
        return ResponseEntity.ok(reportService.getPipelineForecastReport(from, to, ownerId));
    }

    @GetMapping("/expense-analysis")
    @PreAuthorize("hasAuthority('REPORT_VIEW') or hasAuthority('EXPENSE_APPROVE')")
    public ResponseEntity<ExpenseAnalysisReportDto> getExpenseAnalysisReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false, defaultValue = "category") String groupBy) {
        return ResponseEntity.ok(reportService.getExpenseAnalysisReport(from, to, groupBy));
    }

    @GetMapping("/{type}/export")
    @PreAuthorize("hasAuthority('REPORT_VIEW')")
    public ResponseEntity<byte[]> exportReport(
            @PathVariable String type,
            @RequestParam(defaultValue = "pdf") String format,
            @RequestParam Map<String, String> params) {
        byte[] content = reportService.exportReport(type, format, params);
        String filename = type + "-report." + (format.equalsIgnoreCase("xlsx") ? "xlsx" : "pdf");
        MediaType mediaType = format.equalsIgnoreCase("xlsx")
                ? MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                : MediaType.APPLICATION_PDF;

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(mediaType)
                .body(content);
    }
}
