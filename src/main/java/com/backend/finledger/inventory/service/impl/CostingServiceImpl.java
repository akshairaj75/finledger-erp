package com.backend.finledger.inventory.service.impl;

import com.backend.finledger.inventory.entity.Item;
import com.backend.finledger.inventory.entity.StockBalance;
import com.backend.finledger.inventory.repository.ItemRepository;
import com.backend.finledger.inventory.repository.StockBalanceRepository;
import com.backend.finledger.inventory.service.CostingService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class CostingServiceImpl implements CostingService {

    private final StockBalanceRepository stockBalanceRepository;
    private final ItemRepository itemRepository;

    public CostingServiceImpl(StockBalanceRepository stockBalanceRepository, ItemRepository itemRepository) {
        this.stockBalanceRepository = stockBalanceRepository;
        this.itemRepository = itemRepository;
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
}

