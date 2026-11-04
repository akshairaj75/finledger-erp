package com.backend.finledger.crm.dto;

import java.time.Instant;

public record LeadConvertResponse(
    Long leadId,
    String leadNo,
    Long customerId,
    String customerCode,
    String customerName,
    Long contactId,
    String contactName,
    Instant convertedAt,
    boolean matchedExistingCustomer
) {}

