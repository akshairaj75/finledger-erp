package com.backend.finledger.ledger.repository;

import com.backend.finledger.ledger.entity.JournalLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface JournalLineRepository extends JpaRepository<JournalLine, Long> {

    List<JournalLine> findByJournalEntryIdOrderByLineNoAsc(Long journalEntryId);

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
}

