package com.backend.finledger.purchases.repository;

import com.backend.finledger.purchases.entity.Bill;
import com.backend.finledger.purchases.entity.BillStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface BillRepository extends JpaRepository<Bill, Long> {

    Optional<Bill> findByCompanyIdAndId(Long companyId, Long id);

    Optional<Bill> findByCompanyIdAndBillNo(Long companyId, String billNo);

    boolean existsByCompanyIdAndBillNo(Long companyId, String billNo);

    boolean existsByVendorIdAndVendorInvoiceNo(Long vendorId, String vendorInvoiceNo);

    boolean existsByVendorIdAndVendorInvoiceNoAndIdNot(Long vendorId, String vendorInvoiceNo, Long id);

    Page<Bill> findByCompanyId(Long companyId, Pageable pageable);

    Page<Bill> findByCompanyIdAndVendorId(Long companyId, Long vendorId, Pageable pageable);

    List<Bill> findByCompanyIdAndVendorIdAndStatusInAndBalanceDueGreaterThan(
            Long companyId, Long vendorId, Collection<BillStatus> statuses, BigDecimal minBalance);

    List<Bill> findByCompanyIdAndStatusInAndBalanceDueGreaterThan(
            Long companyId, Collection<BillStatus> statuses, BigDecimal minBalance);

    @Query("SELECT b FROM Bill b WHERE b.companyId = :companyId AND b.status <> 'VOID' AND b.status <> 'DRAFT' " +
           "AND b.billDate BETWEEN :fromDate AND :toDate")
    List<Bill> findPostedBillsBetween(
            @Param("companyId") Long companyId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate);
}

