package com.backend.finledger.ledger;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CostCenterRepository extends JpaRepository<CostCenter, Long> {
    Optional<CostCenter> findByCompanyIdAndCode(Long companyId, String code);
    Optional<CostCenter> findByCompanyIdAndId(Long companyId, Long id);
    List<CostCenter> findByCompanyIdOrderByNameAsc(Long companyId);
    List<CostCenter> findByCompanyIdAndIsActiveTrueOrderByNameAsc(Long companyId);
}
