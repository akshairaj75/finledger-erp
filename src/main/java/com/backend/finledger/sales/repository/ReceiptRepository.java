package com.backend.finledger.sales.repository;

import com.backend.finledger.sales.entity.Receipt;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReceiptRepository extends JpaRepository<Receipt, Long> {

    Optional<Receipt> findByCompanyIdAndId(Long companyId, Long id);

    Optional<Receipt> findByCompanyIdAndReceiptNo(Long companyId, String receiptNo);

    Page<Receipt> findByCompanyId(Long companyId, Pageable pageable);

    Page<Receipt> findByCompanyIdAndCustomerId(Long companyId, Long customerId, Pageable pageable);

    @Query("SELECT r FROM Receipt r WHERE r.companyId = :companyId AND r.customerId = :customerId " +
           "AND r.status = 'POSTED' AND r.receiptDate BETWEEN :fromDate AND :toDate ORDER BY r.receiptDate ASC, r.id ASC")
    List<Receipt> findPostedReceiptsForStatement(
            @Param("companyId") Long companyId,
            @Param("customerId") Long customerId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate);

    @Query("SELECT r FROM Receipt r WHERE r.companyId = :companyId AND r.customerId = :customerId " +
           "AND r.status = 'POSTED' AND r.receiptDate < :beforeDate")
    List<Receipt> findPostedReceiptsBeforeDate(
            @Param("companyId") Long companyId,
            @Param("customerId") Long customerId,
            @Param("beforeDate") LocalDate beforeDate);
}

