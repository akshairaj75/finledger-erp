package com.backend.finledger.inventory.dto;

public record ItemCategoryDto(
    Long id,
    Long companyId,
    String name,
    Long parentId,
    String parentName,
    Long incomeAccountId,
    Long inventoryAccountId,
    Long cogsAccountId
) {}

