package com.backend.finledger.inventory.repository;

import com.backend.finledger.inventory.entity.Warehouse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WarehouseRepository extends JpaRepository<Warehouse, Long> {
    Optional<Warehouse> findByCompanyIdAndId(Long companyId, Long id);
    Optional<Warehouse> findByCompanyIdAndCode(Long companyId, String code);
    Optional<Warehouse> findFirstByCompanyIdAndIsDefaultTrue(Long companyId);
    List<Warehouse> findByCompanyId(Long companyId);
    boolean existsByCompanyIdAndCode(Long companyId, String code);
}

