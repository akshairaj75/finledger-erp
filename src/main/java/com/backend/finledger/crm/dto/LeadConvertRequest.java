package com.backend.finledger.crm.dto;

public record LeadConvertRequest(
    String customerCode,
    String customerName,
    String trn,
    Long arAccountId,
    Long matchExistingCustomerId,
    Boolean forceCreate
) {}

