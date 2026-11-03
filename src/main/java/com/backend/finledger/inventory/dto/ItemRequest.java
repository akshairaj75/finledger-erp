package com.backend.finledger.inventory.dto;

import com.backend.finledger.inventory.entity.CostingMethod;
import com.backend.finledger.inventory.entity.ItemType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ItemRequest(
    @NotBlank(message = "SKU is required")
    @Size(max = 40, message = "SKU cannot exceed 40 characters")
    String sku,

    @Size(max = 40, message = "Barcode cannot exceed 40 characters")
    String barcode,

    @NotBlank(message = "Item name is required")
    @Size(max = 150, message = "Item name cannot exceed 150 characters")
    String name,

    Long categoryId,

    @NotNull(message = "Item type is required")
    ItemType itemType,

    String unit,
    CostingMethod costingMethod,
    BigDecimal salesPrice,
    BigDecimal purchasePrice,
    BigDecimal reorderLevel,
    BigDecimal reorderQty,
    Long incomeAccountId,
    Long cogsAccountId,
    Long inventoryAccountId,
    Long taxRateId,
    String imagePath,
    Boolean isActive
) {}

