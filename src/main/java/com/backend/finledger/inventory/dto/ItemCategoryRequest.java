package com.backend.finledger.inventory.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ItemCategoryRequest(
    @NotBlank(message = "Category name is required")
    @Size(max = 100, message = "Category name cannot exceed 100 characters")
    String name,

    Long parentId,
    Long incomeAccountId,
    Long inventoryAccountId,
    Long cogsAccountId
) {}

