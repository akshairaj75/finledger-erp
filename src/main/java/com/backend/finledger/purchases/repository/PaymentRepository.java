package com.backend.finledger.purchases.repository;

import com.backend.finledger.purchases.entity.Payment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByCompanyIdAndId(Long companyId, Long id);

    Optional<Payment> findByCompanyIdAndPaymentNo(Long companyId, String paymentNo);

    boolean existsByCompanyIdAndPaymentNo(Long companyId, String paymentNo);

    Page<Payment> findByCompanyId(Long companyId, Pageable pageable);

    Page<Payment> findByCompanyIdAndVendorId(Long companyId, Long vendorId, Pageable pageable);
}

