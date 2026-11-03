package com.backend.finledger.expense.entity;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * ExpenseApproval entity matching expense_approvals table.
 * Records the multi-level approval history (Line Manager, Finance Review).
 */
@Entity
@Table(name = "expense_approvals", indexes = {
    @Index(name = "ix_exp_appr_expense", columnList = "expense_id")
})
public class ExpenseApproval {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "expense_id", nullable = false)
    private Long expenseId;

    @Column(name = "step", nullable = false)
    private Integer step;

    @Column(name = "approver_id", nullable = false)
    private Long approverId;

    @Column(name = "approver_name", length = 150)
    private String approverName;

    @Enumerated(EnumType.STRING)
    @Column(name = "decision", length = 20, nullable = false)
    private ApprovalDecision decision;

    @Column(name = "comment", length = 500)
    private String comment;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public ExpenseApproval() {}

    public ExpenseApproval(Long expenseId, Integer step, Long approverId, String approverName,
                           ApprovalDecision decision, String comment) {
        this.expenseId = expenseId;
        this.step = step;
        this.approverId = approverId;
        this.approverName = approverName;
        this.decision = decision;
        this.comment = comment;
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getExpenseId() {
        return expenseId;
    }

    public void setExpenseId(Long expenseId) {
        this.expenseId = expenseId;
    }

    public Integer getStep() {
        return step;
    }

    public void setStep(Integer step) {
        this.step = step;
    }

    public Long getApproverId() {
        return approverId;
    }

    public void setApproverId(Long approverId) {
        this.approverId = approverId;
    }

    public String getApproverName() {
        return approverName;
    }

    public void setApproverName(String approverName) {
        this.approverName = approverName;
    }

    public ApprovalDecision getDecision() {
        return decision;
    }

    public void setDecision(ApprovalDecision decision) {
        this.decision = decision;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}

