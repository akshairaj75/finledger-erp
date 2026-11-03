package com.backend.finledger.inventory.service.impl;

import com.backend.finledger.inventory.dto.ReorderSuggestionDto;
import com.backend.finledger.inventory.dto.StockMovementDto;
import com.backend.finledger.inventory.dto.StockSummaryDto;
import com.backend.finledger.inventory.entity.*;
import com.backend.finledger.inventory.mapper.InventoryMapper;
import com.backend.finledger.inventory.repository.*;
import com.backend.finledger.inventory.service.InventoryReportService;
import com.backend.finledger.security.CurrentUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class InventoryReportServiceImpl implements InventoryReportService {

    private final ItemRepository itemRepository;
    private final ItemCategoryRepository categoryRepository;
    private final WarehouseRepository warehouseRepository;
    private final StockBalanceRepository stockBalanceRepository;
    private final StockMovementRepository stockMovementRepository;
    private final InventoryMapper mapper;

    public InventoryReportServiceImpl(
            ItemRepository itemRepository,
            ItemCategoryRepository categoryRepository,
            WarehouseRepository warehouseRepository,
            StockBalanceRepository stockBalanceRepository,
            StockMovementRepository stockMovementRepository,
            InventoryMapper mapper) {
        this.itemRepository = itemRepository;
        this.categoryRepository = categoryRepository;
        this.warehouseRepository = warehouseRepository;
        this.stockBalanceRepository = stockBalanceRepository;
        this.stockMovementRepository = stockMovementRepository;
        this.mapper = mapper;
    }

    @Override
    public List<StockSummaryDto> getStockSummary(Long categoryId, Long warehouseId, String status) {
        Long companyId = CurrentUser.getCompanyId();

        List<Item> items = itemRepository.findByCompanyIdAndIsActiveTrue(companyId);
        if (categoryId != null) {
            items = items.stream().filter(i -> categoryId.equals(i.getCategoryId())).toList();
        }

        Map<Long, String> categoryNames = categoryRepository.findByCompanyId(companyId).stream()
                .collect(Collectors.toMap(ItemCategory::getId, ItemCategory::getName));

        Map<Long, Warehouse> warehousesById = warehouseRepository.findByCompanyId(companyId).stream()
                .collect(Collectors.toMap(Warehouse::getId, w -> w));

        List<Long> itemIds = items.stream().map(Item::getId).toList();
        Map<Long, List<StockBalance>> balancesByItem = stockBalanceRepository.findByItemIdIn(itemIds).stream()
                .collect(Collectors.groupingBy(StockBalance::getItemId));

        List<StockSummaryDto> summaries = new ArrayList<>();

        for (Item item : items) {
            List<StockBalance> balances = balancesByItem.getOrDefault(item.getId(), List.of());

            if (warehouseId != null) {
                balances = balances.stream().filter(b -> warehouseId.equals(b.getWarehouseId())).toList();
            }

            BigDecimal totalOnHand = BigDecimal.ZERO;
            BigDecimal totalReserved = BigDecimal.ZERO;
            BigDecimal totalStockValue = BigDecimal.ZERO;
            BigDecimal avgCost = BigDecimal.ZERO;
            Map<String, BigDecimal> whQuantities = new HashMap<>();

            for (StockBalance b : balances) {
                totalOnHand = totalOnHand.add(b.getQtyOnHand());
                totalReserved = totalReserved.add(b.getQtyReserved());
                totalStockValue = totalStockValue.add(b.getStockValue());
                if (b.getAvgCost().compareTo(BigDecimal.ZERO) > 0) {
                    avgCost = b.getAvgCost();
                }
                Warehouse wh = warehousesById.get(b.getWarehouseId());
                if (wh != null) {
                    whQuantities.put(wh.getCode(), b.getQtyOnHand());
                }
            }

            BigDecimal totalAvailable = totalOnHand.subtract(totalReserved);
            if (totalAvailable.compareTo(BigDecimal.ZERO) < 0) totalAvailable = BigDecimal.ZERO;

            String stockStatus;
            if (totalOnHand.compareTo(BigDecimal.ZERO) <= 0) {
                stockStatus = "OUT_OF_STOCK";
            } else if (item.getReorderLevel().compareTo(BigDecimal.ZERO) > 0 &&
                       totalOnHand.compareTo(item.getReorderLevel()) <= 0) {
                stockStatus = "LOW_STOCK";
            } else {
                stockStatus = "IN_STOCK";
            }

            if (status != null && !status.isBlank() && !status.equalsIgnoreCase("ALL")) {
                if (!stockStatus.equalsIgnoreCase(status.trim())) {
                    continue;
                }
            }

            String catName = item.getCategoryId() != null ? categoryNames.get(item.getCategoryId()) : null;

            summaries.add(new StockSummaryDto(
                    item.getId(),
                    item.getSku(),
                    item.getName(),
                    item.getCategoryId(),
                    catName,
                    item.getItemType(),
                    item.getUnit(),
                    totalOnHand,
                    totalReserved,
                    totalAvailable,
                    avgCost,
                    totalStockValue,
                    item.getReorderLevel(),
                    item.getReorderQty(),
                    stockStatus,
                    whQuantities
            ));
        }

        return summaries;
    }

    @Override
    public List<StockMovementDto> getItemMovements(Long itemId, Long warehouseId, LocalDate from, LocalDate to) {
        Long companyId = CurrentUser.getCompanyId();

        Item item = itemRepository.findByCompanyIdAndId(companyId, itemId).orElse(null);
        String itemSku = item != null ? item.getSku() : "";
        String itemName = item != null ? item.getName() : "";

        Map<Long, Warehouse> whMap = warehouseRepository.findByCompanyId(companyId).stream()
                .collect(Collectors.toMap(Warehouse::getId, w -> w));

        List<StockMovement> movements = stockMovementRepository.findMovements(companyId, itemId, warehouseId, from, to);

        return movements.stream().map(m -> {
            Warehouse wh = whMap.get(m.getWarehouseId());
            String whCode = wh != null ? wh.getCode() : "";
            String whName = wh != null ? wh.getName() : "";

            return new StockMovementDto(
                    m.getId(),
                    m.getCompanyId(),
                    m.getItemId(),
                    itemSku,
                    itemName,
                    m.getWarehouseId(),
                    whCode,
                    whName,
                    m.getMovementDate(),
                    m.getMovementType(),
                    m.getQuantity(),
                    m.getUnitCost(),
                    m.getTotalCost(),
                    m.getBalanceQty(),
                    m.getSourceType(),
                    m.getSourceId(),
                    m.getSourceLineId(),
                    m.getJournalEntryId(),
                    m.getCreatedAt()
            );
        }).toList();
    }

    @Override
    public List<ReorderSuggestionDto> getReorderSuggestions() {
        Long companyId = CurrentUser.getCompanyId();

        List<Item> items = itemRepository.findByCompanyIdAndIsActiveTrue(companyId).stream()
                .filter(i -> i.getItemType() == ItemType.INVENTORY && i.getReorderLevel().compareTo(BigDecimal.ZERO) > 0)
                .toList();

        List<Long> itemIds = items.stream().map(Item::getId).toList();
        Map<Long, List<StockBalance>> balancesByItem = stockBalanceRepository.findByItemIdIn(itemIds).stream()
                .collect(Collectors.groupingBy(StockBalance::getItemId));

        List<ReorderSuggestionDto> suggestions = new ArrayList<>();

        for (Item item : items) {
            List<StockBalance> balances = balancesByItem.getOrDefault(item.getId(), List.of());
            BigDecimal totalOnHand = balances.stream()
                    .map(StockBalance::getQtyOnHand)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            if (totalOnHand.compareTo(item.getReorderLevel()) <= 0) {
                BigDecimal suggestedQty = item.getReorderQty().compareTo(BigDecimal.ZERO) > 0
                        ? item.getReorderQty()
                        : item.getReorderLevel().subtract(totalOnHand);

                BigDecimal estimatedCost = suggestedQty.multiply(item.getPurchasePrice());

                suggestions.add(new ReorderSuggestionDto(
                        item.getId(),
                        item.getSku(),
                        item.getName(),
                        item.getUnit(),
                        totalOnHand,
                        item.getReorderLevel(),
                        item.getReorderQty(),
                        suggestedQty,
                        item.getPurchasePrice(),
                        estimatedCost
                ));
            }
        }

        return suggestions;
    }
}

