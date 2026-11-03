package com.backend.finledger.inventory.repository;

import com.backend.finledger.inventory.entity.StockTransfer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StockTransferRepository extends JpaRepository<StockTransfer, Long> {
    Optional<StockTransfer> findByCompanyIdAndId(Long companyId, Long id);
    Page<StockTransfer> findByCompanyId(Long companyId, Pageable pageable);
    boolean existsByCompanyIdAndTransferNo(Long companyId, String transferNo);
}

