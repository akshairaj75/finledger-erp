package com.backend.finledger.expense.repository;

import com.backend.finledger.expense.entity.ExpenseApproval;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExpenseApprovalRepository extends JpaRepository<ExpenseApproval, Long> {

    List<ExpenseApproval> findByExpenseIdOrderByStepAsc(Long expenseId);

    void deleteByExpenseId(Long expenseId);
}

