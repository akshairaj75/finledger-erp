package com.backend.finledger.crm.dto;

public record ContactDto(
    Long id,
    Long companyId,
    Long customerId,
    Long leadId,
    String fullName,
    String jobTitle,
    String email,
    String phone,
    Boolean isPrimary
) {}

