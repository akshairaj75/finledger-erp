package com.backend.finledger.crm.repository;

import com.backend.finledger.crm.entity.Quotation;
import com.backend.finledger.crm.entity.QuotationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface QuotationRepository extends JpaRepository<Quotation, Long> {

    Optional<Quotation> findByCompanyIdAndId(Long companyId, Long id);

    Optional<Quotation> findByCompanyIdAndQuotationNo(Long companyId, String quotationNo);

    @Query("SELECT q FROM Quotation q WHERE q.companyId = :companyId " +
           "AND (:customerId IS NULL OR q.customerId = :customerId) " +
           "AND (:opportunityId IS NULL OR q.opportunityId = :opportunityId) " +
           "AND (:status IS NULL OR q.status = :status) " +
           "AND (:fromDate IS NULL OR q.quoteDate >= :fromDate) " +
           "AND (:toDate IS NULL OR q.quoteDate <= :toDate) " +
           "AND (:search IS NULL OR LOWER(q.quotationNo) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Quotation> searchQuotations(
            @Param("companyId") Long companyId,
            @Param("customerId") Long customerId,
            @Param("opportunityId") Long opportunityId,
            @Param("status") QuotationStatus status,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            @Param("search") String search,
            Pageable pageable);
}

