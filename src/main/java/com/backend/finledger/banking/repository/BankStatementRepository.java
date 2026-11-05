package com.backend.finledger.banking.repository;

import com.backend.finledger.banking.entity.BankStatement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface BankStatementRepository extends JpaRepository<BankStatement, Long> {
    List<BankStatement> findByBankAccountIdOrderByStatementDateDesc(Long bankAccountId);
    Optional<BankStatement> findByBankAccountIdAndStatementDate(Long bankAccountId, LocalDate statementDate);
}

