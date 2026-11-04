package com.backend.finledger.crm.dto;

import com.backend.finledger.crm.entity.LeadSource;
import com.backend.finledger.crm.entity.LeadStatus;

import java.time.Instant;

public record LeadDto(
    Long id,
    Long companyId,
    String leadNo,
    String companyName,
    String contactName,
    String email,
    String phone,
    LeadSource source,
    String industry,
    String city,
    String countryCode,
    LeadStatus status,
    Long ownerUserId,
    String ownerUserName,
    Long convertedCustomerId,
    String convertedCustomerName,
    Instant convertedAt,
    String notes,
    Instant createdAt
) {}

