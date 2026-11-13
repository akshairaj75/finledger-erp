package com.backend.finledger.expense.repository;

import com.backend.finledger.expense.entity.ExpenseLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExpenseLineRepository extends JpaRepository<ExpenseLine, Long> {

    List<ExpenseLine> findByExpenseIdOrderByLineNoAsc(Long expenseId);

    void deleteByExpenseId(Long expenseId);

    @Query("SELECT el FROM ExpenseLine el JOIN el.expense e " +
           "WHERE e.companyId = :companyId " +
           "AND el.isBillable = true " +
           "AND e.status IN ('APPROVED', 'POSTED', 'REIMBURSED') " +
           "AND (:customerId IS NULL OR el.customerId = :customerId) " +
           "ORDER BY e.expenseDate DESC, el.lineNo ASC")
    List<ExpenseLine> findBillableLines(@Param("companyId") Long companyId, @Param("customerId") Long customerId);
}

