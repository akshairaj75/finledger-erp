package com.backend.finledger.expense.repository;

import com.backend.finledger.expense.entity.ExpenseCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExpenseCategoryRepository extends JpaRepository<ExpenseCategory, Long> {

    List<ExpenseCategory> findByCompanyIdOrderByNameAsc(Long companyId);

    Optional<ExpenseCategory> findByCompanyIdAndId(Long companyId, Long id);

    Optional<ExpenseCategory> findByCompanyIdAndName(Long companyId, String name);

    boolean existsByCompanyIdAndName(Long companyId, String name);

    @Query("SELECT c FROM ExpenseCategory c WHERE c.companyId = :companyId " +
           "AND (:active IS NULL OR c.isActive = :active) " +
           "AND (:q IS NULL OR LOWER(c.name) LIKE LOWER(CONCAT('%', :q, '%')))")
    Page<ExpenseCategory> searchCategories(
            @Param("companyId") Long companyId,
            @Param("active") Boolean active,
            @Param("q") String q,
            Pageable pageable
    );
}

