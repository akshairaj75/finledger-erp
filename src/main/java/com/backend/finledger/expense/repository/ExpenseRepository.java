package com.backend.finledger.expense.repository;

import com.backend.finledger.expense.entity.Expense;
import com.backend.finledger.expense.entity.ExpenseStatus;
import com.backend.finledger.expense.entity.PaidBy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    Optional<Expense> findByCompanyIdAndId(Long companyId, Long id);

    Optional<Expense> findByCompanyIdAndExpenseNo(Long companyId, String expenseNo);

    List<Expense> findByCompanyIdAndIdIn(Long companyId, List<Long> ids);

    @Query("SELECT e FROM Expense e WHERE e.companyId = :companyId " +
           "AND (:status IS NULL OR e.status = :status) " +
           "AND (:paidBy IS NULL OR e.paidBy = :paidBy) " +
           "AND (:employeeUserId IS NULL OR e.employeeUserId = :employeeUserId) " +
           "AND (:fromDate IS NULL OR e.expenseDate >= :fromDate) " +
           "AND (:toDate IS NULL OR e.expenseDate <= :toDate)")
    Page<Expense> searchExpenses(
            @Param("companyId") Long companyId,
            @Param("status") ExpenseStatus status,
            @Param("paidBy") PaidBy paidBy,
            @Param("employeeUserId") Long employeeUserId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            Pageable pageable
    );

    @Query("SELECT COUNT(e) FROM Expense e WHERE e.companyId = :companyId AND e.status = 'SUBMITTED'")
    long countAwaitingApproval(@Param("companyId") Long companyId);
}

