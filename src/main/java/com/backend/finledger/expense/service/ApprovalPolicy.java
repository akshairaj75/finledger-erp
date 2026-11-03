package com.backend.finledger.expense.service;

import com.backend.finledger.expense.entity.Expense;

import java.math.BigDecimal;

public interface ApprovalPolicy {

    /**
     * Enforces category policies upon submission:
     * - Checks if receipt attachments are required and present.
     * - Checks whether any claim line exceeds category limit_per_claim.
     * Throws BusinessException with code RECEIPT_REQUIRED or POLICY_LIMIT_EXCEEDED (HTTP 422).
     */
    void validateSubmission(Expense expense);

    /**
     * Blocks self-approval:
     * Throws ForbiddenException with code SELF_APPROVAL_NOT_ALLOWED (HTTP 403)
     * if the approver attempts to approve their own claim.
     */
    void validateApproval(Expense expense, Long approverUserId);

    /**
     * Returns the number of approval steps required for an expense:
     * 1 step (Line Manager) for claims <= threshold (default 2,000 AED);
     * 2 steps (Line Manager + Finance Review) for claims > threshold.
     */
    int getRequiredApprovalSteps(Expense expense);

    /**
     * Indicates whether the specified step is a Finance review step.
     */
    boolean isFinanceStep(Expense expense, int step);

    BigDecimal getFinanceApprovalThreshold(Long companyId);
}

