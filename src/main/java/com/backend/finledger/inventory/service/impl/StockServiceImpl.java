package com.backend.finledger.inventory.service.impl;

import com.backend.finledger.common.BusinessException;
import com.backend.finledger.common.ForbiddenException;
import com.backend.finledger.company.entity.Company;
import com.backend.finledger.company.repository.CompanyRepository;
import com.backend.finledger.inventory.dto.StockBalanceDto;
import com.backend.finledger.inventory.entity.*;
import com.backend.finledger.inventory.repository.ItemRepository;
import com.backend.finledger.inventory.repository.StockBalanceRepository;
import com.backend.finledger.inventory.repository.StockMovementRepository;
import com.backend.finledger.inventory.repository.WarehouseRepository;
import com.backend.finledger.inventory.service.CostingService;
import com.backend.finledger.inventory.service.StockService;
import com.backend.finledger.security.CurrentUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
public class StockServiceImpl implements StockService {

    private final StockBalanceRepository stockBalanceRepository;
    private final StockMovementRepository stockMovementRepository;
    private final ItemRepository itemRepository;
    private final WarehouseRepository warehouseRepository;
    private final CostingService costingService;
    private final CompanyRepository companyRepository;

    public StockServiceImpl(
            StockBalanceRepository stockBalanceRepository,
            StockMovementRepository stockMovementRepository,
            ItemRepository itemRepository,
            WarehouseRepository warehouseRepository,
            CostingService costingService) {
        this(stockBalanceRepository, stockMovementRepository, itemRepository, warehouseRepository, costingService, null);
    }

    @Autowired
    public StockServiceImpl(
            StockBalanceRepository stockBalanceRepository,
            StockMovementRepository stockMovementRepository,
            ItemRepository itemRepository,
            WarehouseRepository warehouseRepository,
            CostingService costingService,
            CompanyRepository companyRepository) {
        this.stockBalanceRepository = stockBalanceRepository;
        this.stockMovementRepository = stockMovementRepository;
        this.itemRepository = itemRepository;
        this.warehouseRepository = warehouseRepository;
        this.costingService = costingService;
        this.companyRepository = companyRepository;
    }

    @Override
    public StockMovement receiveStock(
            Long companyId,
            Long itemId,
            Long warehouseId,
            BigDecimal quantity,
            BigDecimal unitCost,
            MovementType movementType,
            StockSourceType sourceType,
            Long sourceId,
            Long sourceLineId,
            Long journalEntryId) {

        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("INVALID_QUANTITY", "Received quantity must be greater than zero");
        }

        BigDecimal safeUnitCost = (unitCost != null) ? unitCost.setScale(4, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);

        // Lock row with SELECT ... FOR UPDATE (pessimistic write)
        StockBalance balance = stockBalanceRepository.findByItemIdAndWarehouseIdWithLock(itemId, warehouseId)
                .orElseGet(() -> {
                    StockBalance newBalance = new StockBalance(itemId, warehouseId);
                    return stockBalanceRepository.saveAndFlush(newBalance);
                });

        BigDecimal oldQty = balance.getQtyOnHand();
        BigDecimal oldAvgCost = balance.getAvgCost();

        BigDecimal newAvgCost = costingService.calculateWeightedAverageCost(oldQty, oldAvgCost, quantity, safeUnitCost);
        BigDecimal newQty = oldQty.add(quantity);

        balance.setQtyOnHand(newQty);
        balance.setAvgCost(newAvgCost);
        balance.setStockValue(newQty.multiply(newAvgCost).setScale(2, RoundingMode.HALF_UP));
        stockBalanceRepository.save(balance);

        // Append to immutable stock_movements ledger
        StockMovement movement = new StockMovement();
        movement.setCompanyId(companyId);
        movement.setItemId(itemId);
        movement.setWarehouseId(warehouseId);
        movement.setMovementDate(LocalDate.now());
        movement.setMovementType(movementType);
        movement.setQuantity(quantity);
        movement.setUnitCost(safeUnitCost);
        movement.setTotalCost(quantity.multiply(safeUnitCost).setScale(2, RoundingMode.HALF_UP));
        movement.setBalanceQty(newQty);
        movement.setSourceType(sourceType);
        movement.setSourceId(sourceId);
        movement.setSourceLineId(sourceLineId);
        movement.setJournalEntryId(journalEntryId);
        try {
            movement.setCreatedBy(CurrentUser.getUserId());
        } catch (Exception ignored) {}

        return stockMovementRepository.save(movement);
    }

    @Override
    public StockMovement issueStock(
            Long companyId,
            Long itemId,
            Long warehouseId,
            BigDecimal quantity,
            MovementType movementType,
            StockSourceType sourceType,
            Long sourceId,
            Long sourceLineId,
            Long journalEntryId) {

        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("INVALID_QUANTITY", "Issued quantity must be greater than zero");
        }

        boolean allowNegativeStock = companyRepository != null && Boolean.TRUE.equals(
                companyRepository.findById(companyId).map(Company::getAllowNegativeStock).orElse(false));

        // Lock row with SELECT ... FOR UPDATE (pessimistic write)
        StockBalance balance = stockBalanceRepository.findByItemIdAndWarehouseIdWithLock(itemId, warehouseId)
                .orElseGet(() -> {
                    if (!allowNegativeStock) {
                        throw new BusinessException("INSUFFICIENT_STOCK",
                                "No stock record found for item " + itemId + " in warehouse " + warehouseId);
                    }
                    Item item = itemRepository.findById(itemId).orElse(null);
                    BigDecimal fallbackCost = (item != null && item.getPurchasePrice() != null)
                            ? item.getPurchasePrice() : BigDecimal.ZERO;
                    StockBalance created = new StockBalance(itemId, warehouseId);
                    created.setQtyOnHand(BigDecimal.ZERO);
                    created.setAvgCost(fallbackCost);
                    created.setStockValue(BigDecimal.ZERO);
                    return created;
                });

        BigDecimal currentQty = balance.getQtyOnHand();
        if (!allowNegativeStock && currentQty.compareTo(quantity) < 0) {
            Item item = itemRepository.findById(itemId).orElse(null);
            Warehouse wh = warehouseRepository.findById(warehouseId).orElse(null);
            String itemLabel = (item != null) ? item.getSku() : String.valueOf(itemId);
            String whLabel = (wh != null) ? wh.getCode() : String.valueOf(warehouseId);
            throw new BusinessException("INSUFFICIENT_STOCK",
                    "Insufficient stock for item " + itemLabel + " in warehouse " + whLabel +
                    ". Available on hand: " + currentQty + ", requested: " + quantity);
        }

        Item item = itemRepository.findById(itemId).orElse(null);
        BigDecimal currentAvgCost = balance.getAvgCost();
        BigDecimal unitCost;
        if (item != null && item.getCostingMethod() == CostingMethod.FIFO) {
            unitCost = costingService.calculateIssueUnitCost(companyId, itemId, warehouseId, quantity);
        } else {
            unitCost = currentAvgCost;
        }

        BigDecimal newQty = currentQty.subtract(quantity);
        balance.setQtyOnHand(newQty);
        if (item != null && item.getCostingMethod() == CostingMethod.FIFO) {
            BigDecimal totalCostVal = quantity.multiply(unitCost).setScale(2, RoundingMode.HALF_UP);
            BigDecimal newValue = balance.getStockValue().subtract(totalCostVal);
            if (newValue.compareTo(BigDecimal.ZERO) < 0 || newQty.compareTo(BigDecimal.ZERO) <= 0) {
                newValue = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            }
            balance.setStockValue(newValue);
            if (newQty.compareTo(BigDecimal.ZERO) > 0) {
                balance.setAvgCost(newValue.divide(newQty, 4, RoundingMode.HALF_UP));
            }
        } else {
            balance.setStockValue(newQty.multiply(currentAvgCost).setScale(2, RoundingMode.HALF_UP));
        }
        stockBalanceRepository.save(balance);

        // Append to immutable stock_movements ledger (negative qty indicates stock decrement)
        StockMovement movement = new StockMovement();
        movement.setCompanyId(companyId);
        movement.setItemId(itemId);
        movement.setWarehouseId(warehouseId);
        movement.setMovementDate(LocalDate.now());
        movement.setMovementType(movementType);
        movement.setQuantity(quantity.negate());
        movement.setUnitCost(unitCost);
        movement.setTotalCost(quantity.multiply(unitCost).setScale(2, RoundingMode.HALF_UP));
        movement.setBalanceQty(newQty);
        movement.setSourceType(sourceType);
        movement.setSourceId(sourceId);
        movement.setSourceLineId(sourceLineId);
        movement.setJournalEntryId(journalEntryId);
        try {
            movement.setCreatedBy(CurrentUser.getUserId());
        } catch (Exception ignored) {}

        return stockMovementRepository.save(movement);
    }

    @Override
    @Transactional(readOnly = true)
    public StockBalanceDto getStockBalance(Long itemId, Long warehouseId) {
        Long companyId = CurrentUser.getCompanyId();
        Item item = requireCompanyItem(companyId, itemId);
        Warehouse wh = requireCompanyWarehouse(companyId, warehouseId);
        StockBalance balance = stockBalanceRepository.findByCompanyIdAndItemIdAndWarehouseId(companyId, itemId, warehouseId)
                .orElse(new StockBalance(itemId, warehouseId));
        return toDto(balance, item, wh);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StockBalanceDto> getStockBalancesByItem(Long itemId) {
        Long companyId = CurrentUser.getCompanyId();
        Item item = requireCompanyItem(companyId, itemId);
        return stockBalanceRepository.findByCompanyIdAndItemId(companyId, itemId).stream()
                .map(sb -> toDto(sb, item, warehouseRepository.findByCompanyIdAndId(companyId, sb.getWarehouseId()).orElse(null)))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<StockBalanceDto> getStockBalancesByWarehouse(Long warehouseId) {
        Long companyId = CurrentUser.getCompanyId();
        Warehouse wh = requireCompanyWarehouse(companyId, warehouseId);
        return stockBalanceRepository.findByCompanyIdAndWarehouseId(companyId, warehouseId).stream()
                .map(sb -> toDto(sb, itemRepository.findByCompanyIdAndId(companyId, sb.getItemId()).orElse(null), wh))
                .toList();
    }

    private Item requireCompanyItem(Long companyId, Long itemId) {
        return itemRepository.findByCompanyIdAndId(companyId, itemId)
                .orElseThrow(() -> itemRepository.existsById(itemId)
                        ? new ForbiddenException("ACCESS_DENIED", "Item does not belong to current company: " + itemId)
                        : new BusinessException("ITEM_NOT_FOUND", "Item not found with id: " + itemId));
    }

    private Warehouse requireCompanyWarehouse(Long companyId, Long warehouseId) {
        return warehouseRepository.findByCompanyIdAndId(companyId, warehouseId)
                .orElseThrow(() -> warehouseRepository.existsById(warehouseId)
                        ? new ForbiddenException("ACCESS_DENIED", "Warehouse does not belong to current company: " + warehouseId)
                        : new BusinessException("WAREHOUSE_NOT_FOUND", "Warehouse not found with id: " + warehouseId));
    }

    private StockBalanceDto toDto(StockBalance balance, Item item, Warehouse wh) {

        String itemSku = item != null ? item.getSku() : "";
        String itemName = item != null ? item.getName() : "";
        String whCode = wh != null ? wh.getCode() : "";
        String whName = wh != null ? wh.getName() : "";

        BigDecimal available = balance.getQtyOnHand().subtract(balance.getQtyReserved());
        if (available.compareTo(BigDecimal.ZERO) < 0) available = BigDecimal.ZERO;

        return new StockBalanceDto(
                balance.getId(),
                balance.getItemId(),
                itemSku,
                itemName,
                balance.getWarehouseId(),
                whCode,
                whName,
                balance.getQtyOnHand(),
                balance.getQtyReserved(),
                available,
                balance.getAvgCost(),
                balance.getStockValue(),
                balance.getUpdatedAt()
        );
    }
}

