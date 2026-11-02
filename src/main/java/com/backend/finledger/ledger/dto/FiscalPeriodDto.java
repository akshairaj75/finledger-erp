package com.backend.finledger.ledger.dto;

import com.backend.finledger.ledger.FiscalPeriodStatus;
import java.time.LocalDate;

public record FiscalPeriodDto(
    Long id,
    Long fiscalYearId,
    Byte periodNo,
    LocalDate startDate,
    LocalDate endDate,
    FiscalPeriodStatus status
) {}
