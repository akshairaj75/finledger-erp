package com.backend.finledger.crm.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

public record QuotationRequest(
    Long opportunityId,
    Long customerId,
    Long leadId,

    @NotNull(message = "Quote date is required")
    LocalDate quoteDate,

    @NotNull(message = "Valid until date is required")
    LocalDate validUntil,

    String currencyCode,

    @NotEmpty(message = "Quotation must contain at least one line item")
    @Valid
    List<QuotationLineRequest> lines
) {}

