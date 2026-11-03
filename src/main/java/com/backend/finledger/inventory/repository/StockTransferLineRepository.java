package com.backend.finledger.inventory.repository;

import com.backend.finledger.inventory.entity.StockTransferLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StockTransferLineRepository extends JpaRepository<StockTransferLine, Long> {
    List<StockTransferLine> findByTransferId(Long transferId);
}

