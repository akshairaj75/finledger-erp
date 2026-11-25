package com.backend.finledger.banking.repository;

import com.backend.finledger.banking.entity.BankStatement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface BankStatementRepository extends JpaRepository<BankStatement, Long> {
    List<BankStatement> findByBankAccountIdOrderByStatementDateDesc(Long bankAccountId);
    Optional<BankStatement> findByBankAccountIdAndStatementDate(Long bankAccountId, LocalDate statementDate);

    @Query("SELECT bs FROM BankStatement bs JOIN BankAccount ba ON bs.bankAccountId = ba.id " +
           "WHERE ba.companyId = :companyId AND bs.id = :id")
    Optional<BankStatement> findByCompanyIdAndId(@Param("companyId") Long companyId, @Param("id") Long id);

    @Query("SELECT bs FROM BankStatement bs JOIN BankAccount ba ON bs.bankAccountId = ba.id " +
           "WHERE ba.companyId = :companyId AND bs.bankAccountId = :bankAccountId " +
           "ORDER BY bs.statementDate DESC")
    List<BankStatement> findByCompanyIdAndBankAccountIdOrderByStatementDateDesc(
            @Param("companyId") Long companyId,
            @Param("bankAccountId") Long bankAccountId);
}

