package com.backend.finledger.ledger.repository;

import com.backend.finledger.ledger.entity.JournalEntry;
import com.backend.finledger.ledger.entity.JournalSourceType;
import com.backend.finledger.ledger.entity.JournalStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface JournalEntryRepository extends JpaRepository<JournalEntry, Long> {

    Optional<JournalEntry> findByCompanyIdAndId(Long companyId, Long id);

    Optional<JournalEntry> findByCompanyIdAndEntryNo(Long companyId, String entryNo);

    boolean existsByCompanyIdAndReversalOfId(Long companyId, Long reversalOfId);

    @Query("SELECT j FROM JournalEntry j WHERE j.companyId = :companyId " +
           "AND (:from IS NULL OR j.entryDate >= :from) " +
           "AND (:to IS NULL OR j.entryDate <= :to) " +
           "AND (:status IS NULL OR j.status = :status) " +
           "AND (:sourceType IS NULL OR j.sourceType = :sourceType)")
    Page<JournalEntry> searchJournalEntries(
            @Param("companyId") Long companyId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to,
            @Param("status") JournalStatus status,
            @Param("sourceType") JournalSourceType sourceType,
            Pageable pageable
    );
}

