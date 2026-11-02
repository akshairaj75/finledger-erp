package com.backend.finledger.ledger;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {

    List<Account> findByCompanyIdOrderByCodeAsc(Long companyId);

    Optional<Account> findByCompanyIdAndId(Long companyId, Long id);

    Optional<Account> findByCompanyIdAndCode(Long companyId, String code);

    @Query("SELECT a FROM Account a WHERE a.companyId = :companyId " +
           "AND (:accountType IS NULL OR a.accountType = :accountType) " +
           "AND (:active IS NULL OR a.isActive = :active) " +
           "AND (:q IS NULL OR LOWER(a.name) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(a.code) LIKE LOWER(CONCAT('%', :q, '%')))")
    Page<Account> searchAccounts(
            @Param("companyId") Long companyId,
            @Param("accountType") AccountType accountType,
            @Param("active") Boolean active,
            @Param("q") String q,
            Pageable pageable
    );

    @Query(value = "SELECT COUNT(*) FROM journal_lines WHERE account_id = :accountId", nativeQuery = true)
    long countJournalLinesByAccountId(@Param("accountId") Long accountId);
}

