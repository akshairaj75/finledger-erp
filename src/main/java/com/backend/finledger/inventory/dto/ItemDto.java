package com.backend.finledger.inventory.dto;

import com.backend.finledger.inventory.entity.CostingMethod;
import com.backend.finledger.inventory.entity.ItemType;

import java.math.BigDecimal;

public record ItemDto(
    Long id,
    Long companyId,
    String sku,
    String barcode,
    String name,
    Long categoryId,
    String categoryName,
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
    Boolean isActive,
    BigDecimal totalOnHand,
    BigDecimal totalStockValue
) {}

