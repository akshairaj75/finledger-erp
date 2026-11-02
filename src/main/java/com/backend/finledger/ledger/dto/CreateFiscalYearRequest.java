package com.backend.finledger.ledger.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record CreateFiscalYearRequest(
    @NotBlank(message = "Fiscal year name is required")
    String name,

    @NotNull(message = "Start date is required")
    LocalDate startDate,

    @NotNull(message = "End date is required")
    LocalDate endDate,

    Boolean autoGeneratePeriods
) {}
