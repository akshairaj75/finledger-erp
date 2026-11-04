package com.backend.finledger.crm.dto;

import jakarta.validation.constraints.NotBlank;

public record ContactRequest(
    Long customerId,
    Long leadId,

    @NotBlank(message = "Contact name is required")
    String fullName,

    String jobTitle,
    String email,
    String phone,
    Boolean isPrimary
) {}

