package com.backend.finledger.inventory.repository;

import com.backend.finledger.inventory.entity.StockBalance;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface StockBalanceRepository extends JpaRepository<StockBalance, Long> {

    Optional<StockBalance> findByItemIdAndWarehouseId(Long itemId, Long warehouseId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT sb FROM StockBalance sb WHERE sb.itemId = :itemId AND sb.warehouseId = :warehouseId")
    Optional<StockBalance> findByItemIdAndWarehouseIdWithLock(@Param("itemId") Long itemId, @Param("warehouseId") Long warehouseId);

    List<StockBalance> findByItemId(Long itemId);

    List<StockBalance> findByWarehouseId(Long warehouseId);

    List<StockBalance> findByItemIdIn(Collection<Long> itemIds);
}

