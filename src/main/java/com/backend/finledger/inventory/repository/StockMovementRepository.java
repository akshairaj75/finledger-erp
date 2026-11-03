package com.backend.finledger.inventory.repository;

import com.backend.finledger.inventory.entity.StockMovement;
import com.backend.finledger.inventory.entity.StockSourceType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {

    Page<StockMovement> findByCompanyIdAndItemId(Long companyId, Long itemId, Pageable pageable);

    @Query("SELECT sm FROM StockMovement sm WHERE sm.companyId = :companyId AND sm.itemId = :itemId " +
           "AND (:warehouseId IS NULL OR sm.warehouseId = :warehouseId) " +
           "AND (:from IS NULL OR sm.movementDate >= :from) " +
           "AND (:to IS NULL OR sm.movementDate <= :to) " +
           "ORDER BY sm.movementDate ASC, sm.id ASC")
    List<StockMovement> findMovements(
            @Param("companyId") Long companyId,
            @Param("itemId") Long itemId,
            @Param("warehouseId") Long warehouseId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);

    List<StockMovement> findByCompanyIdAndSourceTypeAndSourceId(Long companyId, StockSourceType sourceType, Long sourceId);
}

