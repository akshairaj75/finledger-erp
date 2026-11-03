package com.backend.finledger.expense.service.impl;

import com.backend.finledger.attachment.repository.AttachmentRepository;
import com.backend.finledger.common.BusinessException;
import com.backend.finledger.common.ForbiddenException;
import com.backend.finledger.expense.entity.Expense;
import com.backend.finledger.expense.entity.ExpenseCategory;
import com.backend.finledger.expense.entity.ExpenseLine;
import com.backend.finledger.expense.repository.ExpenseCategoryRepository;
import com.backend.finledger.expense.service.ApprovalPolicy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class ApprovalPolicyImpl implements ApprovalPolicy {

    private final AttachmentRepository attachmentRepository;
    private final ExpenseCategoryRepository expenseCategoryRepository;
    private final BigDecimal defaultThreshold;

    public ApprovalPolicyImpl(
            AttachmentRepository attachmentRepository,
            ExpenseCategoryRepository expenseCategoryRepository,
            @Value("${finledger.expense.finance-approval-threshold:2000.00}") BigDecimal defaultThreshold) {
        this.attachmentRepository = attachmentRepository;
        this.expenseCategoryRepository = expenseCategoryRepository;
        this.defaultThreshold = defaultThreshold != null ? defaultThreshold : new BigDecimal("2000.00");
    }

    @Override
    public void validateSubmission(Expense expense) {
        if (expense.getLines() == null || expense.getLines().isEmpty()) {
            throw new BusinessException("EMPTY_EXPENSE", "Expense claim must have at least one line.");
        }

        Long companyId = expense.getCompanyId();

        for (ExpenseLine line : expense.getLines()) {
            ExpenseCategory category = expenseCategoryRepository.findByCompanyIdAndId(companyId, line.getCategoryId())
                    .orElseThrow(() -> new BusinessException("CATEGORY_NOT_FOUND",
                            "Expense category not found with id: " + line.getCategoryId()));

            // 1. Receipt requirement check
            if (Boolean.TRUE.equals(category.getReceiptRequired())) {
                long attachmentCount = attachmentRepository.countByCompanyIdAndEntityTypeAndEntityId(
                        companyId, "EXPENSE", expense.getId());
                if (attachmentCount == 0) {
                    throw new BusinessException("RECEIPT_REQUIRED",
                            "Receipt attachment is required for expense category: " + category.getName());
                }
            }

            // 2. Limit per claim check
            if (category.getLimitPerClaim() != null && category.getLimitPerClaim().compareTo(BigDecimal.ZERO) > 0) {
                if (line.getAmount().compareTo(category.getLimitPerClaim()) > 0) {
                    throw new BusinessException("POLICY_LIMIT_EXCEEDED",
                            "Line amount " + line.getAmount() + " exceeds policy limit of "
                                    + category.getLimitPerClaim() + " for category: " + category.getName());
                }
            }
        }
    }

    @Override
    public void validateApproval(Expense expense, Long approverUserId) {
        if (approverUserId == null) {
            throw new ForbiddenException("UNAUTHENTICATED", "Active user is required for expense approval.");
        }

        // Block self-approval: approver cannot approve their own claim
        if (approverUserId.equals(expense.getEmployeeUserId()) || approverUserId.equals(expense.getCreatedBy())) {
            throw new ForbiddenException("SELF_APPROVAL_NOT_ALLOWED",
                    "Approver cannot approve their own expense claim.");
        }
    }

    @Override
    public int getRequiredApprovalSteps(Expense expense) {
        BigDecimal threshold = getFinanceApprovalThreshold(expense.getCompanyId());
        if (expense.getTotal() != null && expense.getTotal().compareTo(threshold) > 0) {
            return 2; // Step 1: Line manager, Step 2: Finance review
        }
        return 1; // Step 1: Line manager
    }

    @Override
    public boolean isFinanceStep(Expense expense, int step) {
        return getRequiredApprovalSteps(expense) == 2 && step == 2;
    }

    @Override
    public BigDecimal getFinanceApprovalThreshold(Long companyId) {
        return defaultThreshold;
    }
}

