package com.backend.finledger.expense.dto;

import com.backend.finledger.expense.entity.PaidBy;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record CreateExpenseRequest(
    @NotNull(message = "Expense date is required")
    LocalDate expenseDate,

    @NotNull(message = "Paid-by is required")
    PaidBy paidBy,

    Long employeeUserId,
    Long vendorId,
    String payeeName,
    Long bankAccountId,
    String currencyCode,
    BigDecimal exchangeRate,

    @NotEmpty(message = "Expense must contain at least one line")
    @Valid
    List<ExpenseLineRequest> lines
) {}

