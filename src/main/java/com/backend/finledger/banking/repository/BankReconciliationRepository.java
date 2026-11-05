package com.backend.finledger.banking.repository;

import com.backend.finledger.banking.entity.BankReconciliation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface BankReconciliationRepository extends JpaRepository<BankReconciliation, Long> {
    List<BankReconciliation> findByBankAccountIdOrderByStatementDateDesc(Long bankAccountId);
    Optional<BankReconciliation> findByBankAccountIdAndStatementDate(Long bankAccountId, LocalDate statementDate);
}

