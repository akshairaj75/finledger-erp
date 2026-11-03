package com.backend.finledger.report.service;

import com.backend.finledger.ledger.dto.GeneralLedgerReportDto;
import com.backend.finledger.ledger.dto.TrialBalanceReportDto;

import java.time.LocalDate;

public interface ReportService {
    GeneralLedgerReportDto getGeneralLedgerReport(Long accountId, LocalDate from, LocalDate to);
    TrialBalanceReportDto getTrialBalanceReport(LocalDate asOf);
}

