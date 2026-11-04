package com.backend.finledger.sales.repository;

import com.backend.finledger.sales.entity.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {

    Optional<Customer> findByCompanyIdAndId(Long companyId, Long id);

    Optional<Customer> findByCompanyIdAndCode(Long companyId, String code);

    Optional<Customer> findFirstByCompanyIdAndTrn(Long companyId, String trn);

    Optional<Customer> findFirstByCompanyIdAndEmailIgnoreCase(Long companyId, String email);

    boolean existsByCompanyIdAndCode(Long companyId, String code);

    Page<Customer> findByCompanyId(Long companyId, Pageable pageable);

    Page<Customer> findByCompanyIdAndNameContainingIgnoreCase(Long companyId, String name, Pageable pageable);
}

