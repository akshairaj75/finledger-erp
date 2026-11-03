package com.backend.finledger.ledger.service;

import com.backend.finledger.ledger.dto.CreateFiscalYearRequest;
import com.backend.finledger.ledger.dto.FiscalPeriodDto;
import com.backend.finledger.ledger.dto.FiscalYearDto;

import java.util.List;

public interface FiscalYearPeriodService {
    List<FiscalYearDto> getFiscalYears();
    FiscalYearDto createFiscalYear(CreateFiscalYearRequest request);
    FiscalPeriodDto closePeriod(Long periodId);
    FiscalPeriodDto lockPeriod(Long periodId);
}

