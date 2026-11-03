package com.backend.finledger.inventory.dto;

public record WarehouseDto(
    Long id,
    Long companyId,
    String code,
    String name,
    String address,
    Long costCenterId,
    Boolean isDefault,
    Boolean isActive
) {}

