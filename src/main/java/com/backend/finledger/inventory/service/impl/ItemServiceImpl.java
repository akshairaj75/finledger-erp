package com.backend.finledger.inventory.service.impl;

import com.backend.finledger.common.BusinessException;
import com.backend.finledger.common.PageResponse;
import com.backend.finledger.inventory.dto.ItemDto;
import com.backend.finledger.inventory.dto.ItemRequest;
import com.backend.finledger.inventory.entity.CostingMethod;
import com.backend.finledger.inventory.entity.Item;
import com.backend.finledger.inventory.entity.ItemCategory;
import com.backend.finledger.inventory.entity.ItemType;
import com.backend.finledger.inventory.entity.StockBalance;
import com.backend.finledger.inventory.mapper.InventoryMapper;
import com.backend.finledger.inventory.repository.ItemCategoryRepository;
import com.backend.finledger.inventory.repository.ItemRepository;
import com.backend.finledger.inventory.repository.StockBalanceRepository;
import com.backend.finledger.inventory.service.ItemService;
import com.backend.finledger.security.CurrentUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
public class ItemServiceImpl implements ItemService {

    private final ItemRepository itemRepository;
    private final ItemCategoryRepository categoryRepository;
    private final StockBalanceRepository stockBalanceRepository;
    private final InventoryMapper mapper;

    public ItemServiceImpl(
            ItemRepository itemRepository,
            ItemCategoryRepository categoryRepository,
            StockBalanceRepository stockBalanceRepository,
            InventoryMapper mapper) {
        this.itemRepository = itemRepository;
        this.categoryRepository = categoryRepository;
        this.stockBalanceRepository = stockBalanceRepository;
        this.mapper = mapper;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ItemDto> searchItems(ItemType itemType, Long categoryId, Boolean active, String search, Pageable pageable) {
        Long companyId = CurrentUser.getCompanyId();
        String searchPattern = (search != null && !search.isBlank()) ? search.trim() : null;

        Page<Item> page = itemRepository.searchItems(companyId, itemType, categoryId, active, searchPattern, pageable);
        List<Long> itemIds = page.getContent().stream().map(Item::getId).toList();

        Map<Long, List<StockBalance>> balancesByItem = stockBalanceRepository.findByItemIdIn(itemIds).stream()
                .collect(Collectors.groupingBy(StockBalance::getItemId));

        Map<Long, String> categoryNames = categoryRepository.findByCompanyId(companyId).stream()
                .collect(Collectors.toMap(ItemCategory::getId, ItemCategory::getName));

        List<ItemDto> dtoList = page.getContent().stream().map(item -> {
            List<StockBalance> balances = balancesByItem.getOrDefault(item.getId(), List.of());
            BigDecimal totalOnHand = balances.stream()
                    .map(StockBalance::getQtyOnHand)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal totalValue = balances.stream()
                    .map(StockBalance::getStockValue)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            String catName = item.getCategoryId() != null ? categoryNames.get(item.getCategoryId()) : null;

            return new ItemDto(
                    item.getId(),
                    item.getCompanyId(),
                    item.getSku(),
                    item.getBarcode(),
                    item.getName(),
                    item.getCategoryId(),
                    catName,
                    item.getItemType(),
                    item.getUnit(),
                    item.getCostingMethod(),
                    item.getSalesPrice(),
                    item.getPurchasePrice(),
                    item.getReorderLevel(),
                    item.getReorderQty(),
                    item.getIncomeAccountId(),
                    item.getCogsAccountId(),
                    item.getInventoryAccountId(),
                    item.getTaxRateId(),
                    item.getImagePath(),
                    item.getIsActive(),
                    totalOnHand,
                    totalValue
            );
        }).toList();

        return new PageResponse<>(dtoList, page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }

    @Override
    @Transactional(readOnly = true)
    public ItemDto getItemById(Long id) {
        Long companyId = CurrentUser.getCompanyId();
        Item item = itemRepository.findByCompanyIdAndId(companyId, id)
                .orElseThrow(() -> new BusinessException("ITEM_NOT_FOUND", "Item not found with id: " + id));
        return enrichItemDto(item, companyId);
    }

    @Override
    @Transactional(readOnly = true)
    public ItemDto getItemBySku(String sku) {
        Long companyId = CurrentUser.getCompanyId();
        Item item = itemRepository.findByCompanyIdAndSku(companyId, sku.trim())
                .orElseThrow(() -> new BusinessException("ITEM_NOT_FOUND", "Item not found with SKU: " + sku));
        return enrichItemDto(item, companyId);
    }

    @Override
    public ItemDto createItem(ItemRequest request) {
        Long companyId = CurrentUser.getCompanyId();
        String sku = request.sku().trim().toUpperCase();

        if (itemRepository.existsByCompanyIdAndSku(companyId, sku)) {
            throw new BusinessException("DUPLICATE_SKU", "Item already exists with SKU: " + sku);
        }

        Item item = mapper.toItemEntity(request);
        item.setCompanyId(companyId);
        item.setSku(sku);
        item.setName(request.name().trim());
        if (request.costingMethod() == null) {
            item.setCostingMethod(CostingMethod.WEIGHTED_AVERAGE);
        }
        if (request.unit() == null || request.unit().isBlank()) {
            item.setUnit("PCS");
        }
        if (request.salesPrice() == null) item.setSalesPrice(BigDecimal.ZERO);
        if (request.purchasePrice() == null) item.setPurchasePrice(BigDecimal.ZERO);
        if (request.reorderLevel() == null) item.setReorderLevel(BigDecimal.ZERO);
        if (request.reorderQty() == null) item.setReorderQty(BigDecimal.ZERO);
        if (request.isActive() == null) item.setIsActive(true);

        // Inherit default GL accounts from category if not set
        if (item.getCategoryId() != null) {
            categoryRepository.findByCompanyIdAndId(companyId, item.getCategoryId()).ifPresent(cat -> {
                if (item.getIncomeAccountId() == null) item.setIncomeAccountId(cat.getIncomeAccountId());
                if (item.getCogsAccountId() == null) item.setCogsAccountId(cat.getCogsAccountId());
                if (item.getInventoryAccountId() == null) item.setInventoryAccountId(cat.getInventoryAccountId());
            });
        }

        Item savedItem = itemRepository.save(item);
        return enrichItemDto(savedItem, companyId);
    }

    @Override
    public ItemDto updateItem(Long id, ItemRequest request) {
        Long companyId = CurrentUser.getCompanyId();
        Item item = itemRepository.findByCompanyIdAndId(companyId, id)
                .orElseThrow(() -> new BusinessException("ITEM_NOT_FOUND", "Item not found with id: " + id));

        String sku = request.sku().trim().toUpperCase();
        if (!item.getSku().equalsIgnoreCase(sku) && itemRepository.existsByCompanyIdAndSku(companyId, sku)) {
            throw new BusinessException("DUPLICATE_SKU", "Item already exists with SKU: " + sku);
        }

        item.setSku(sku);
        item.setName(request.name().trim());
        item.setBarcode(request.barcode());
        item.setCategoryId(request.categoryId());
        item.setItemType(request.itemType());
        if (request.unit() != null && !request.unit().isBlank()) item.setUnit(request.unit());
        if (request.costingMethod() != null) item.setCostingMethod(request.costingMethod());
        if (request.salesPrice() != null) item.setSalesPrice(request.salesPrice());
        if (request.purchasePrice() != null) item.setPurchasePrice(request.purchasePrice());
        if (request.reorderLevel() != null) item.setReorderLevel(request.reorderLevel());
        if (request.reorderQty() != null) item.setReorderQty(request.reorderQty());
        item.setIncomeAccountId(request.incomeAccountId());
        item.setCogsAccountId(request.cogsAccountId());
        item.setInventoryAccountId(request.inventoryAccountId());
        item.setTaxRateId(request.taxRateId());
        item.setImagePath(request.imagePath());
        if (request.isActive() != null) item.setIsActive(request.isActive());

        Item savedItem = itemRepository.save(item);
        return enrichItemDto(savedItem, companyId);
    }

    @Override
    public void deleteItem(Long id) {
        Long companyId = CurrentUser.getCompanyId();
        Item item = itemRepository.findByCompanyIdAndId(companyId, id)
                .orElseThrow(() -> new BusinessException("ITEM_NOT_FOUND", "Item not found with id: " + id));

        // If item has stock, block deletion
        List<StockBalance> balances = stockBalanceRepository.findByItemId(id);
        boolean hasStock = balances.stream().anyMatch(b -> b.getQtyOnHand().compareTo(BigDecimal.ZERO) != 0);
        if (hasStock) {
            throw new BusinessException("ITEM_HAS_STOCK", "Cannot delete item with positive or negative on-hand stock");
        }

        itemRepository.delete(item);
    }

    private ItemDto enrichItemDto(Item item, Long companyId) {
        List<StockBalance> balances = stockBalanceRepository.findByItemId(item.getId());
        BigDecimal totalOnHand = balances.stream()
                .map(StockBalance::getQtyOnHand)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalValue = balances.stream()
                .map(StockBalance::getStockValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        String catName = null;
        if (item.getCategoryId() != null) {
            catName = categoryRepository.findByCompanyIdAndId(companyId, item.getCategoryId())
                    .map(ItemCategory::getName)
                    .orElse(null);
        }

        return new ItemDto(
                item.getId(),
                item.getCompanyId(),
                item.getSku(),
                item.getBarcode(),
                item.getName(),
                item.getCategoryId(),
                catName,
                item.getItemType(),
                item.getUnit(),
                item.getCostingMethod(),
                item.getSalesPrice(),
                item.getPurchasePrice(),
                item.getReorderLevel(),
                item.getReorderQty(),
                item.getIncomeAccountId(),
                item.getCogsAccountId(),
                item.getInventoryAccountId(),
                item.getTaxRateId(),
                item.getImagePath(),
                item.getIsActive(),
                totalOnHand,
                totalValue
        );
    }
}

