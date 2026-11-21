package com.backend.finledger.inventory.service.impl;

import com.backend.finledger.inventory.entity.Item;
import com.backend.finledger.inventory.entity.MovementType;
import com.backend.finledger.inventory.entity.StockBalance;
import com.backend.finledger.inventory.entity.StockMovement;
import com.backend.finledger.inventory.repository.ItemRepository;
import com.backend.finledger.inventory.repository.StockBalanceRepository;
import com.backend.finledger.inventory.repository.StockMovementRepository;
import com.backend.finledger.inventory.service.CostingService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Set;

@Service
public class CostingServiceImpl implements CostingService {

    private static final Set<MovementType> INBOUND_TYPES = Set.of(
            MovementType.OPENING,
            MovementType.PURCHASE,
            MovementType.ADJUSTMENT_IN,
            MovementType.TRANSFER_IN
    );

    private static final Set<MovementType> OUTBOUND_TYPES = Set.of(
            MovementType.SALE,
            MovementType.PURCHASE_RETURN,
            MovementType.ADJUSTMENT_OUT,
            MovementType.TRANSFER_OUT
    );

    private final StockBalanceRepository stockBalanceRepository;
    private final ItemRepository itemRepository;
    private final StockMovementRepository stockMovementRepository;

    public CostingServiceImpl(
            StockBalanceRepository stockBalanceRepository,
            ItemRepository itemRepository,
            StockMovementRepository stockMovementRepository) {
        this.stockBalanceRepository = stockBalanceRepository;
        this.itemRepository = itemRepository;
        this.stockMovementRepository = stockMovementRepository;
    }

    @Override
    public BigDecimal calculateWeightedAverageCost(
            BigDecimal oldQty,
            BigDecimal oldAvgCost,
            BigDecimal receivedQty,
            BigDecimal unitCost) {

        BigDecimal safeOldQty = (oldQty != null) ? oldQty : BigDecimal.ZERO;
        BigDecimal safeOldAvg = (oldAvgCost != null) ? oldAvgCost : BigDecimal.ZERO;
        BigDecimal safeReceivedQty = (receivedQty != null) ? receivedQty : BigDecimal.ZERO;
        BigDecimal safeUnitCost = (unitCost != null) ? unitCost.setScale(4, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);

        if (safeOldQty.compareTo(BigDecimal.ZERO) <= 0) {
            return safeUnitCost;
        }

        BigDecimal totalQty = safeOldQty.add(safeReceivedQty);
        if (totalQty.compareTo(BigDecimal.ZERO) <= 0) {
            return safeUnitCost;
        }

        BigDecimal oldValue = safeOldQty.multiply(safeOldAvg);
        BigDecimal receivedValue = safeReceivedQty.multiply(safeUnitCost);
        BigDecimal totalValue = oldValue.add(receivedValue);

        return totalValue.divide(totalQty, 4, RoundingMode.HALF_UP);
    }

    @Override
    public BigDecimal getCurrentAverageCost(Long itemId, Long warehouseId) {
        if (itemId == null) return BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);

        if (warehouseId != null) {
            var optBalance = stockBalanceRepository.findByItemIdAndWarehouseId(itemId, warehouseId);
            if (optBalance.isPresent() && optBalance.get().getAvgCost().compareTo(BigDecimal.ZERO) > 0) {
                return optBalance.get().getAvgCost().setScale(4, RoundingMode.HALF_UP);
            }
        }

        // Fallback: check other warehouses for this item
        var balances = stockBalanceRepository.findByItemId(itemId);
        for (StockBalance b : balances) {
            if (b.getAvgCost() != null && b.getAvgCost().compareTo(BigDecimal.ZERO) > 0) {
                return b.getAvgCost().setScale(4, RoundingMode.HALF_UP);
            }
        }

        // Fallback: check item purchase price
        var optItem = itemRepository.findById(itemId);
        if (optItem.isPresent() && optItem.get().getPurchasePrice() != null) {
            return optItem.get().getPurchasePrice().setScale(4, RoundingMode.HALF_UP);
        }

        return BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
    }

    @Override
    public BigDecimal calculateIssueUnitCost(Long companyId, Long itemId, Long warehouseId, BigDecimal issueQty) {
        if (issueQty == null || issueQty.compareTo(BigDecimal.ZERO) <= 0) {
            return getCurrentAverageCost(itemId, warehouseId);
        }

        List<StockMovement> movements = stockMovementRepository.findMovementsByItemAndWarehouse(companyId, itemId, warehouseId);

        BigDecimal cumulativeOutbound = BigDecimal.ZERO;
        for (StockMovement m : movements) {
            if (OUTBOUND_TYPES.contains(m.getMovementType()) || (m.getQuantity() != null && m.getQuantity().compareTo(BigDecimal.ZERO) < 0)) {
                cumulativeOutbound = cumulativeOutbound.add(m.getQuantity().abs());
            }
        }

        BigDecimal consumedOutbound = cumulativeOutbound;
        BigDecimal remainingToIssue = issueQty;
        BigDecimal totalCost = BigDecimal.ZERO;

        for (StockMovement m : movements) {
            if (INBOUND_TYPES.contains(m.getMovementType()) && m.getQuantity() != null && m.getQuantity().compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal layerQty = m.getQuantity();
                BigDecimal layerCost = (m.getUnitCost() != null) ? m.getUnitCost() : BigDecimal.ZERO;

                if (consumedOutbound.compareTo(layerQty) >= 0) {
                    consumedOutbound = consumedOutbound.subtract(layerQty);
                } else {
                    BigDecimal availableInLayer = layerQty.subtract(consumedOutbound);
                    consumedOutbound = BigDecimal.ZERO;

                    BigDecimal take = remainingToIssue.min(availableInLayer);
                    totalCost = totalCost.add(take.multiply(layerCost));
                    remainingToIssue = remainingToIssue.subtract(take);

                    if (remainingToIssue.compareTo(BigDecimal.ZERO) <= 0) {
                        break;
                    }
                }
            }
        }

        // Fall back to current average cost if insufficient inbound layers exist
        if (remainingToIssue.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal fallbackCost = getCurrentAverageCost(itemId, warehouseId);
            totalCost = totalCost.add(remainingToIssue.multiply(fallbackCost));
        }

        return totalCost.divide(issueQty, 4, RoundingMode.HALF_UP);
    }
}

