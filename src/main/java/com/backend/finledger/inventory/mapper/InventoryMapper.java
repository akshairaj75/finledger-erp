package com.backend.finledger.inventory.mapper;

import com.backend.finledger.inventory.dto.*;
import com.backend.finledger.inventory.entity.*;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface InventoryMapper {

    @Mapping(target = "parentName", ignore = true)
    ItemCategoryDto toCategoryDto(ItemCategory category);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "companyId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "version", ignore = true)
    ItemCategory toCategoryEntity(ItemCategoryRequest request);

    WarehouseDto toWarehouseDto(Warehouse warehouse);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "companyId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "version", ignore = true)
    Warehouse toWarehouseEntity(WarehouseRequest request);

    @Mapping(target = "categoryName", ignore = true)
    @Mapping(target = "totalOnHand", ignore = true)
    @Mapping(target = "totalStockValue", ignore = true)
    ItemDto toItemDto(Item item);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "companyId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "version", ignore = true)
    Item toItemEntity(ItemRequest request);

    @Mapping(target = "itemSku", ignore = true)
    @Mapping(target = "itemName", ignore = true)
    @Mapping(target = "warehouseCode", ignore = true)
    @Mapping(target = "warehouseName", ignore = true)
    @Mapping(target = "qtyAvailable", ignore = true)
    StockBalanceDto toStockBalanceDto(StockBalance balance);

    @Mapping(target = "itemSku", ignore = true)
    @Mapping(target = "itemName", ignore = true)
    @Mapping(target = "warehouseCode", ignore = true)
    @Mapping(target = "warehouseName", ignore = true)
    StockMovementDto toStockMovementDto(StockMovement movement);

    @Mapping(target = "itemSku", ignore = true)
    @Mapping(target = "itemName", ignore = true)
    StockAdjustmentLineDto toAdjustmentLineDto(StockAdjustmentLine line);

    List<StockAdjustmentLineDto> toAdjustmentLineDtoList(List<StockAdjustmentLine> lines);

    @Mapping(target = "warehouseName", ignore = true)
    @Mapping(target = "adjustmentAccountName", ignore = true)
    @Mapping(target = "journalEntryNo", ignore = true)
    StockAdjustmentDto toAdjustmentDto(StockAdjustment adjustment);

    @Mapping(target = "itemSku", ignore = true)
    @Mapping(target = "itemName", ignore = true)
    StockTransferLineDto toTransferLineDto(StockTransferLine line);

    List<StockTransferLineDto> toTransferLineDtoList(List<StockTransferLine> lines);

    @Mapping(target = "fromWarehouseName", ignore = true)
    @Mapping(target = "toWarehouseName", ignore = true)
    StockTransferDto toTransferDto(StockTransfer transfer);
}

