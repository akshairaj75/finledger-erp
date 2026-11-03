package com.backend.finledger.inventory.service;

import com.backend.finledger.inventory.dto.StockBalanceDto;
import com.backend.finledger.inventory.entity.MovementType;
import com.backend.finledger.inventory.entity.StockMovement;
import com.backend.finledger.inventory.entity.StockSourceType;

import java.math.BigDecimal;
import java.util.List;

public interface StockService {

    /**
     * Receives stock into a warehouse:
     * - Locks the (item_id, warehouse_id) row in stock_balances with pessimistic lock (SELECT ... FOR UPDATE)
     * - Recalculates weighted-average cost via CostingService
     * - Updates qty_on_hand, avg_cost, and stock_value
     * - Writes an entry to the append-only stock_movements ledger
     */
    StockMovement receiveStock(
            Long companyId,
            Long itemId,
            Long warehouseId,
            BigDecimal quantity,
            BigDecimal unitCost,
            MovementType movementType,
            StockSourceType sourceType,
            Long sourceId,
            Long sourceLineId,
            Long journalEntryId);

    /**
     * Issues stock from a warehouse:
     * - Locks the (item_id, warehouse_id) row in stock_balances with pessimistic lock (SELECT ... FOR UPDATE)
     * - Blocks negative stock by default unless permitted
     * - Keeps current average cost intact
     * - Decrements qty_on_hand and updates stock_value
     * - Writes an entry to the append-only stock_movements ledger
     */
    StockMovement issueStock(
            Long companyId,
            Long itemId,
            Long warehouseId,
            BigDecimal quantity,
            MovementType movementType,
            StockSourceType sourceType,
            Long sourceId,
            Long sourceLineId,
            Long journalEntryId);

    StockBalanceDto getStockBalance(Long itemId, Long warehouseId);

    List<StockBalanceDto> getStockBalancesByItem(Long itemId);

    List<StockBalanceDto> getStockBalancesByWarehouse(Long warehouseId);
}

