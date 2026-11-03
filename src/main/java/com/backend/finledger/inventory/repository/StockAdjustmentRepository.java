package com.backend.finledger.inventory.repository;

import com.backend.finledger.inventory.entity.StockAdjustment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StockAdjustmentRepository extends JpaRepository<StockAdjustment, Long> {
    Optional<StockAdjustment> findByCompanyIdAndId(Long companyId, Long id);
    Page<StockAdjustment> findByCompanyId(Long companyId, Pageable pageable);
    boolean existsByCompanyIdAndAdjustmentNo(Long companyId, String adjustmentNo);
}

