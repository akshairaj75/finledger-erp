package com.backend.finledger.expense.dto;

import java.math.BigDecimal;
import java.util.List;

public record BatchReimburseResponse(
    Long reimbursementEntryId,
    String journalEntryNo,
    BigDecimal totalAmount,
    int settledExpenseCount,
    List<Long> expenseIds
) {}

