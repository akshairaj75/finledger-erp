package com.backend.finledger.inventory.service;

import java.math.BigDecimal;

public interface CostingService {

    /**
     * Calculates the new weighted-average unit cost on receipt:
     * new_avg = (old_qty * old_avg + received_qty * unit_cost) / (old_qty + received_qty)
     */
    BigDecimal calculateWeightedAverageCost(
            BigDecimal oldQty,
            BigDecimal oldAvgCost,
            BigDecimal receivedQty,
            BigDecimal unitCost);

    /**
     * Retrieves the current average cost for the item in the specified warehouse.
     */
    BigDecimal getCurrentAverageCost(Long itemId, Long warehouseId);
}

