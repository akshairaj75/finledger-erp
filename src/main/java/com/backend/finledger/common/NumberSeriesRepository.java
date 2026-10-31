package com.backend.finledger.common;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NumberSeriesRepository extends JpaRepository<NumberSeries, Long> {

    /**
     * Finds number series with pessimistic write lock (SELECT ... FOR UPDATE)
     * to guarantee gap-free sequence generation under high concurrency.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT ns FROM NumberSeries ns WHERE ns.companyId = :companyId AND ns.docType = :docType")
    Optional<NumberSeries> findByCompanyIdAndDocTypeForUpdate(
        @Param("companyId") Long companyId,
        @Param("docType") DocType docType
    );

    Optional<NumberSeries> findByCompanyIdAndDocType(Long companyId, DocType docType);
}

