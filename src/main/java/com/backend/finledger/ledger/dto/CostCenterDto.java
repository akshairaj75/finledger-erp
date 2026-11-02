package com.backend.finledger.ledger.dto;

public record CostCenterDto(
    Long id,
    Long companyId,
    String code,
    String name,
    Long parentId,
    Boolean isActive
) {}
