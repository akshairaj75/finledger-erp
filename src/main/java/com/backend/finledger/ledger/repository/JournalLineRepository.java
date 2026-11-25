package com.backend.finledger.ledger.repository;

import com.backend.finledger.ledger.entity.JournalLine;
import com.backend.finledger.ledger.entity.JournalSourceType;
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
public interface JournalLineRepository extends JpaRepository<JournalLine, Long> {

    List<JournalLine> findByJournalEntryIdOrderByLineNoAsc(Long journalEntryId);

    @Query("SELECT jl FROM JournalLine jl JOIN jl.journalEntry je WHERE je.companyId = :companyId AND jl.id = :id")
    Optional<JournalLine> findByCompanyIdAndId(@Param("companyId") Long companyId, @Param("id") Long id);

    @Query("SELECT jl FROM JournalLine jl JOIN jl.journalEntry je WHERE je.companyId = :companyId AND jl.id IN :ids")
    List<JournalLine> findByCompanyIdAndIdIn(@Param("companyId") Long companyId, @Param("ids") Collection<Long> ids);

    @Query("SELECT COALESCE(SUM(CASE WHEN jl.isReconciled = true THEN 1 ELSE 0 END), 0) " +
           "FROM JournalLine jl JOIN jl.journalEntry je WHERE je.companyId = :companyId AND je.id = :journalEntryId")
    long countReconciledLinesByJournalEntryId(@Param("companyId") Long companyId, @Param("journalEntryId") Long journalEntryId);

    @Query("SELECT jl FROM JournalLine jl JOIN jl.journalEntry je " +
           "WHERE je.companyId = :companyId " +
           "AND jl.accountId = :accountId " +
           "AND je.status = 'POSTED' " +
           "AND je.entryDate BETWEEN :from AND :to " +
           "ORDER BY je.entryDate ASC, je.id ASC, jl.lineNo ASC")
    List<JournalLine> findPostedLinesForAccount(
            @Param("companyId") Long companyId,
            @Param("accountId") Long accountId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to
    );

    @Query("SELECT COALESCE(SUM(jl.baseDebit), 0) FROM JournalLine jl JOIN jl.journalEntry je " +
           "WHERE je.companyId = :companyId " +
           "AND jl.accountId = :accountId " +
           "AND je.status = 'POSTED' " +
           "AND je.entryDate < :beforeDate")
    BigDecimal sumBaseDebitBeforeDate(
            @Param("companyId") Long companyId,
            @Param("accountId") Long accountId,
            @Param("beforeDate") LocalDate beforeDate
    );

    @Query("SELECT COALESCE(SUM(jl.baseCredit), 0) FROM JournalLine jl JOIN jl.journalEntry je " +
           "WHERE je.companyId = :companyId " +
           "AND jl.accountId = :accountId " +
           "AND je.status = 'POSTED' " +
           "AND je.entryDate < :beforeDate")
    BigDecimal sumBaseCreditBeforeDate(
            @Param("companyId") Long companyId,
            @Param("accountId") Long accountId,
            @Param("beforeDate") LocalDate beforeDate
    );

    interface AccountBalanceSummary {
        Long getAccountId();
        BigDecimal getTotalDebit();
        BigDecimal getTotalCredit();
    }

    @Query("SELECT jl.accountId AS accountId, " +
           "COALESCE(SUM(jl.baseDebit), 0) AS totalDebit, " +
           "COALESCE(SUM(jl.baseCredit), 0) AS totalCredit " +
           "FROM JournalLine jl JOIN jl.journalEntry je " +
           "WHERE je.companyId = :companyId " +
           "AND je.status = 'POSTED' " +
           "AND je.entryDate <= :asOfDate " +
           "GROUP BY jl.accountId")
    List<AccountBalanceSummary> getAccountBalanceSummaries(
            @Param("companyId") Long companyId,
            @Param("asOfDate") LocalDate asOfDate
    );

    @Query("SELECT jl.accountId AS accountId, " +
           "COALESCE(SUM(jl.baseDebit), 0) AS totalDebit, " +
           "COALESCE(SUM(jl.baseCredit), 0) AS totalCredit " +
           "FROM JournalLine jl JOIN jl.journalEntry je " +
           "WHERE je.companyId = :companyId " +
           "AND je.status = 'POSTED' " +
           "AND je.entryDate BETWEEN :from AND :to " +
           "AND (:costCenterId IS NULL OR jl.costCenterId = :costCenterId) " +
           "GROUP BY jl.accountId")
    List<AccountBalanceSummary> getAccountBalanceSummariesBetween(
            @Param("companyId") Long companyId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to,
            @Param("costCenterId") Long costCenterId
    );

    @Query("SELECT jl.accountId AS accountId, " +
           "COALESCE(SUM(jl.baseDebit), 0) AS totalDebit, " +
           "COALESCE(SUM(jl.baseCredit), 0) AS totalCredit " +
           "FROM JournalLine jl JOIN jl.journalEntry je " +
           "WHERE je.companyId = :companyId " +
           "AND je.status = 'POSTED' " +
           "AND je.sourceType <> :excludedSourceType " +
           "AND je.entryDate BETWEEN :from AND :to " +
           "AND (:costCenterId IS NULL OR jl.costCenterId = :costCenterId) " +
           "GROUP BY jl.accountId")
    List<AccountBalanceSummary> getAccountBalanceSummariesBetweenExcludingSourceType(
            @Param("companyId") Long companyId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to,
            @Param("costCenterId") Long costCenterId,
            @Param("excludedSourceType") JournalSourceType excludedSourceType
    );

    @Query("SELECT jl FROM JournalLine jl JOIN jl.journalEntry je " +
           "WHERE je.companyId = :companyId " +
           "AND jl.accountId = :accountId " +
           "AND je.status = 'POSTED' " +
           "AND je.entryDate <= :asOfDate " +
           "AND jl.isReconciled = false " +
           "ORDER BY je.entryDate ASC, jl.id ASC")
    List<JournalLine> findUnreconciledLinesForAccount(
            @Param("companyId") Long companyId,
            @Param("accountId") Long accountId,
            @Param("asOfDate") LocalDate asOfDate
    );

    @Query("SELECT jl FROM JournalLine jl JOIN jl.journalEntry je " +
           "WHERE je.companyId = :companyId " +
           "AND jl.accountId = :accountId " +
           "AND je.status = 'POSTED' " +
           "AND je.entryDate <= :asOfDate " +
           "ORDER BY je.entryDate ASC, jl.id ASC")
    List<JournalLine> findAllPostedLinesForAccount(
            @Param("companyId") Long companyId,
            @Param("accountId") Long accountId,
            @Param("asOfDate") LocalDate asOfDate
    );
}
