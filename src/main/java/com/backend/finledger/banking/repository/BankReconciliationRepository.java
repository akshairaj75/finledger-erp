package com.backend.finledger.banking.repository;

import com.backend.finledger.banking.entity.BankReconciliation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface BankReconciliationRepository extends JpaRepository<BankReconciliation, Long> {
    List<BankReconciliation> findByBankAccountIdOrderByStatementDateDesc(Long bankAccountId);
    Optional<BankReconciliation> findByBankAccountIdAndStatementDate(Long bankAccountId, LocalDate statementDate);

    @Query("SELECT br FROM BankReconciliation br JOIN BankAccount ba ON br.bankAccountId = ba.id " +
           "WHERE ba.companyId = :companyId AND br.id = :id")
    Optional<BankReconciliation> findByCompanyIdAndId(@Param("companyId") Long companyId, @Param("id") Long id);

    @Query("SELECT br FROM BankReconciliation br JOIN BankAccount ba ON br.bankAccountId = ba.id " +
           "WHERE ba.companyId = :companyId AND br.bankAccountId = :bankAccountId " +
           "ORDER BY br.statementDate DESC")
    List<BankReconciliation> findByCompanyIdAndBankAccountIdOrderByStatementDateDesc(
            @Param("companyId") Long companyId,
            @Param("bankAccountId") Long bankAccountId);
}

