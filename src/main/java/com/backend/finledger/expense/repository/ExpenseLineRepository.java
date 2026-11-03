package com.backend.finledger.expense.repository;

import com.backend.finledger.expense.entity.ExpenseLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExpenseLineRepository extends JpaRepository<ExpenseLine, Long> {

    List<ExpenseLine> findByExpenseIdOrderByLineNoAsc(Long expenseId);

    void deleteByExpenseId(Long expenseId);
}

