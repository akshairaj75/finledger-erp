package com.backend.finledger.sales.repository;

import com.backend.finledger.sales.entity.Invoice;
import com.backend.finledger.sales.entity.InvoiceStatus;
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
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    Optional<Invoice> findByCompanyIdAndId(Long companyId, Long id);

    Optional<Invoice> findByCompanyIdAndInvoiceNo(Long companyId, String invoiceNo);

    boolean existsByCompanyIdAndInvoiceNo(Long companyId, String invoiceNo);

    Page<Invoice> findByCompanyId(Long companyId, Pageable pageable);

    Page<Invoice> findByCompanyIdAndCustomerId(Long companyId, Long customerId, Pageable pageable);

    List<Invoice> findByCompanyIdAndCustomerIdAndStatusInAndBalanceDueGreaterThan(
            Long companyId, Long customerId, Collection<InvoiceStatus> statuses, BigDecimal minBalance);

    List<Invoice> findByCompanyIdAndStatusInAndBalanceDueGreaterThan(
            Long companyId, Collection<InvoiceStatus> statuses, BigDecimal minBalance);

    @Query("SELECT i FROM Invoice i WHERE i.companyId = :companyId AND i.customerId = :customerId " +
           "AND i.status <> 'VOID' AND i.status <> 'DRAFT' " +
           "AND i.invoiceDate BETWEEN :fromDate AND :toDate ORDER BY i.invoiceDate ASC, i.id ASC")
    List<Invoice> findPostedInvoicesForStatement(
            @Param("companyId") Long companyId,
            @Param("customerId") Long customerId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate);

    @Query("SELECT i FROM Invoice i WHERE i.companyId = :companyId AND i.customerId = :customerId " +
           "AND i.status <> 'VOID' AND i.status <> 'DRAFT' " +
           "AND i.invoiceDate < :beforeDate")
    List<Invoice> findPostedInvoicesBeforeDate(
            @Param("companyId") Long companyId,
            @Param("customerId") Long customerId,
            @Param("beforeDate") LocalDate beforeDate);
}

