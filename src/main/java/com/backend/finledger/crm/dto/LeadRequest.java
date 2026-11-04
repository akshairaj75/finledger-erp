package com.backend.finledger.crm.dto;

import com.backend.finledger.crm.entity.LeadSource;
import jakarta.validation.constraints.NotBlank;

public record LeadRequest(
    String companyName,

    @NotBlank(message = "Contact name is required")
    String contactName,

    String email,
    String phone,
    LeadSource source,
    String industry,
    String city,
    String countryCode,
    Long ownerUserId,
    String notes
) {}

