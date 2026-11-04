package com.backend.finledger.crm.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record OpportunityDto(
    Long id,
    Long companyId,
    String opportunityNo,
    String title,
    Long leadId,
    Long customerId,
    String customerName,
    Long contactId,
    String contactName,
    Long stageId,
    String stageName,
    BigDecimal expectedValue,
    String currencyCode,
    Byte probabilityPct,
    LocalDate expectedCloseDate,
    Long ownerUserId,
    String ownerUserName,
    String lostReason,
    Instant closedAt,
    Instant createdAt
) {}

