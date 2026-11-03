package com.backend.finledger.inventory.repository;

import com.backend.finledger.inventory.entity.StockAdjustmentLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StockAdjustmentLineRepository extends JpaRepository<StockAdjustmentLine, Long> {
    List<StockAdjustmentLine> findByAdjustmentId(Long adjustmentId);
}

