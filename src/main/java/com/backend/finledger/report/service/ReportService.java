package com.backend.finledger.report.service;

import com.backend.finledger.ledger.dto.GeneralLedgerReportDto;
import com.backend.finledger.ledger.dto.TrialBalanceReportDto;
import com.backend.finledger.report.dto.*;

import java.time.LocalDate;
import java.util.Map;

public interface ReportService {
    GeneralLedgerReportDto getGeneralLedgerReport(Long accountId, LocalDate from, LocalDate to);

    TrialBalanceReportDto getTrialBalanceReport(LocalDate asOf);

    DashboardSummaryDto getDashboardSummary(LocalDate from, LocalDate to);

    ProfitLossReportDto getProfitLossReport(LocalDate from, LocalDate to, String compare, Long costCenterId);

    BalanceSheetReportDto getBalanceSheetReport(LocalDate asOf, String compare);

    VatReturnReportDto getVatReturnReport(LocalDate from, LocalDate to);

    StockValuationReportDto getStockValuationReport(LocalDate asOf, Long warehouseId);

    StockMovementReportDto getStockMovementReport(Long itemId, LocalDate from, LocalDate to);

    PipelineForecastReportDto getPipelineForecastReport(LocalDate from, LocalDate to, Long ownerId);

    ExpenseAnalysisReportDto getExpenseAnalysisReport(LocalDate from, LocalDate to, String groupBy);

    byte[] exportReport(String type, String format, Map<String, String> params);
}
