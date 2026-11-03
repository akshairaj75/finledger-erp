package com.backend.finledger.tax.repository;

import com.backend.finledger.tax.entity.TaxRate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TaxRateRepository extends JpaRepository<TaxRate, Long> {
    List<TaxRate> findByCompanyId(Long companyId);
    Optional<TaxRate> findByCompanyIdAndCode(Long companyId, String code);
    Optional<TaxRate> findByCompanyIdAndId(Long companyId, Long id);
}

