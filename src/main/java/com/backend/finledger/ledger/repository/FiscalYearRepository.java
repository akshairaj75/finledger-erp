package com.backend.finledger.ledger.repository;

import com.backend.finledger.ledger.entity.FiscalYear;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FiscalYearRepository extends JpaRepository<FiscalYear, Long> {
    Optional<FiscalYear> findByCompanyIdAndName(Long companyId, String name);
    Optional<FiscalYear> findByCompanyIdAndId(Long companyId, Long id);
    List<FiscalYear> findByCompanyIdOrderByStartDateDesc(Long companyId);
}

