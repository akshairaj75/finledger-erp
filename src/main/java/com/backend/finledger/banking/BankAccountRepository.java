package com.backend.finledger.banking;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BankAccountRepository extends JpaRepository<BankAccount, Long> {

    Optional<BankAccount> findByCompanyIdAndId(Long companyId, Long id);

    List<BankAccount> findByCompanyIdAndIsActiveTrue(Long companyId);
}

