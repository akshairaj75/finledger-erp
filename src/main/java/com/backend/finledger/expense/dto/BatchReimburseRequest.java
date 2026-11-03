package com.backend.finledger.expense.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;

public record BatchReimburseRequest(
    @NotEmpty(message = "At least one expense must be selected for reimbursement")
    List<Long> expenseIds,

    @NotNull(message = "Bank/Cash account is required for payment")
    Long bankAccountId,

    LocalDate paymentDate
) {}

