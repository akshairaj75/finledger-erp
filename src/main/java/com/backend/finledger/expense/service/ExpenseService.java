package com.backend.finledger.expense.service;

import com.backend.finledger.common.PageResponse;
import com.backend.finledger.expense.dto.CreateExpenseRequest;
import com.backend.finledger.expense.dto.ExpenseDto;
import com.backend.finledger.expense.dto.UpdateExpenseRequest;
import com.backend.finledger.expense.entity.ExpenseStatus;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

public interface ExpenseService {

    PageResponse<ExpenseDto> getExpenses(
            String tab,
            ExpenseStatus status,
            LocalDate from,
            LocalDate to,
            Pageable pageable
    );

    ExpenseDto getExpenseById(Long id);

    ExpenseDto createExpense(CreateExpenseRequest request);

    ExpenseDto updateExpense(Long id, UpdateExpenseRequest request);

    ExpenseDto submitExpense(Long id);

    ExpenseDto approveExpense(Long id, String comment);

    ExpenseDto rejectExpense(Long id, String reason);
}

