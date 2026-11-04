package com.backend.finledger.crm.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record OpportunityRequest(
    @NotBlank(message = "Opportunity title is required")
    String title,

    Long leadId,
    Long customerId,
    Long contactId,

    @NotNull(message = "Pipeline stage is required")
    Long stageId,

    BigDecimal expectedValue,
    String currencyCode,
    Byte probabilityPct,
    LocalDate expectedCloseDate,
    Long ownerUserId
) {}

