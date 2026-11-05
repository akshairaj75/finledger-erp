package com.backend.finledger.banking.repository;

import com.backend.finledger.banking.entity.BankStatementLine;
import com.backend.finledger.banking.entity.MatchStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BankStatementLineRepository extends JpaRepository<BankStatementLine, Long> {
    List<BankStatementLine> findByBankStatementIdOrderByTxnDateAsc(Long bankStatementId);
    List<BankStatementLine> findByBankStatementIdAndMatchStatus(Long bankStatementId, MatchStatus matchStatus);
}

