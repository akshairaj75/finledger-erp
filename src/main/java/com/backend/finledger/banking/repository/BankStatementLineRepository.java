package com.backend.finledger.banking.repository;

import com.backend.finledger.banking.entity.BankStatementLine;
import com.backend.finledger.banking.entity.MatchStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BankStatementLineRepository extends JpaRepository<BankStatementLine, Long> {
    List<BankStatementLine> findByBankStatementIdOrderByTxnDateAsc(Long bankStatementId);
    List<BankStatementLine> findByBankStatementIdAndMatchStatus(Long bankStatementId, MatchStatus matchStatus);

    @Query("SELECT bsl FROM BankStatementLine bsl JOIN bsl.bankStatement bs JOIN BankAccount ba ON bs.bankAccountId = ba.id " +
           "WHERE ba.companyId = :companyId AND bsl.id = :id")
    Optional<BankStatementLine> findByCompanyIdAndId(@Param("companyId") Long companyId, @Param("id") Long id);
}

