package com.backend.finledger.crm.repository;

import com.backend.finledger.crm.entity.QuotationLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuotationLineRepository extends JpaRepository<QuotationLine, Long> {

    List<QuotationLine> findByQuotationIdOrderByLineNoAsc(Long quotationId);
}

