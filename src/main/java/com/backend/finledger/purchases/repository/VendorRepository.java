package com.backend.finledger.purchases.repository;

import com.backend.finledger.purchases.entity.Vendor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VendorRepository extends JpaRepository<Vendor, Long> {

    Optional<Vendor> findByCompanyIdAndId(Long companyId, Long id);

    Optional<Vendor> findByCompanyIdAndCode(Long companyId, String code);

    boolean existsByCompanyIdAndCode(Long companyId, String code);

    Page<Vendor> findByCompanyId(Long companyId, Pageable pageable);

    Page<Vendor> findByCompanyIdAndNameContainingIgnoreCase(Long companyId, String name, Pageable pageable);
}

