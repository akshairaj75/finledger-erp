package com.backend.finledger.ledger.dto;

import com.backend.finledger.ledger.FiscalYearStatus;
import java.time.LocalDate;
import java.util.List;

public record FiscalYearDto(
    Long id,
    Long companyId,
    String name,
    LocalDate startDate,
    LocalDate endDate,
    FiscalYearStatus status,
    List<FiscalPeriodDto> periods
) {}
