package com.backend.finledger.sales.repository;

import com.backend.finledger.sales.entity.InvoiceLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InvoiceLineRepository extends JpaRepository<InvoiceLine, Long> {

    List<InvoiceLine> findByInvoiceIdOrderByLineNoAsc(Long invoiceId);

    @Query("SELECT il FROM InvoiceLine il JOIN il.invoice i WHERE i.companyId = :companyId AND il.id = :id")
    Optional<InvoiceLine> findByCompanyIdAndId(@Param("companyId") Long companyId, @Param("id") Long id);

    @Query("SELECT il FROM InvoiceLine il JOIN il.invoice i WHERE i.companyId = :companyId AND il.invoice.id = :invoiceId ORDER BY il.lineNo ASC")
    List<InvoiceLine> findByCompanyIdAndInvoiceIdOrderByLineNoAsc(@Param("companyId") Long companyId, @Param("invoiceId") Long invoiceId);
}

