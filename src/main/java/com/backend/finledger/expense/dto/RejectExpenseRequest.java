package com.backend.finledger.expense.dto;

import jakarta.validation.constraints.NotBlank;

public record RejectExpenseRequest(
    @NotBlank(message = "Rejection reason is required")
    String reason
) {}

