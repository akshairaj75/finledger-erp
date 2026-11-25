package com.backend.finledger.purchases.repository;

import com.backend.finledger.purchases.entity.BillLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BillLineRepository extends JpaRepository<BillLine, Long> {

    List<BillLine> findByBillId(Long billId);

    @Query("SELECT bl FROM BillLine bl JOIN bl.bill b WHERE b.companyId = :companyId AND bl.id = :id")
    Optional<BillLine> findByCompanyIdAndId(@Param("companyId") Long companyId, @Param("id") Long id);

    @Query("SELECT bl FROM BillLine bl JOIN bl.bill b WHERE b.companyId = :companyId AND bl.bill.id = :billId ORDER BY bl.lineNo ASC")
    List<BillLine> findByCompanyIdAndBillId(@Param("companyId") Long companyId, @Param("billId") Long billId);
}

