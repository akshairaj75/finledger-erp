package com.backend.finledger.expense.dto;

import java.time.Instant;

public record ExpenseApprovalDto(
    Integer step,
    String approver,
    String decision,
    Instant at,
    String comment
) {}

