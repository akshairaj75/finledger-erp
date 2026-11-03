package com.backend.finledger.expense.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;

/**
 * ExpenseLine entity matching expense_lines table.
 * Individual expense items within a claim or company expense.
 */
@Entity
@Table(name = "expense_lines", indexes = {
    @Index(name = "ix_expl_exp", columnList = "expense_id")
})
public class ExpenseLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "expense_id", nullable = false)
    private Expense expense;

    @Column(name = "line_no", nullable = false)
    private Short lineNo;

    @Column(name = "category_id", nullable = false)
    private Long categoryId;

    @Column(name = "account_id", nullable = false)
    private Long accountId;

    @Column(name = "description", length = 255, nullable = false)
    private String description;

    @Column(name = "amount", precision = 18, scale = 2, nullable = false)
    private BigDecimal amount = BigDecimal.ZERO;

    @Column(name = "tax_rate_id")
    private Long taxRateId;

    @Column(name = "tax_amount", precision = 18, scale = 2, nullable = false)
    private BigDecimal taxAmount = BigDecimal.ZERO;

    @Column(name = "cost_center_id")
    private Long costCenterId;

    @Column(name = "customer_id")
    private Long customerId;

    @Column(name = "is_billable", nullable = false)
    private Boolean isBillable = false;

    public ExpenseLine() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Expense getExpense() {
        return expense;
    }

    public void setExpense(Expense expense) {
        this.expense = expense;
    }

    public Short getLineNo() {
        return lineNo;
    }

    public void setLineNo(Short lineNo) {
        this.lineNo = lineNo;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public Long getTaxRateId() {
        return taxRateId;
    }

    public void setTaxRateId(Long taxRateId) {
        this.taxRateId = taxRateId;
    }

    public BigDecimal getTaxAmount() {
        return taxAmount;
    }

    public void setTaxAmount(BigDecimal taxAmount) {
        this.taxAmount = taxAmount;
    }

    public Long getCostCenterId() {
        return costCenterId;
    }

    public void setCostCenterId(Long costCenterId) {
        this.costCenterId = costCenterId;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public Boolean getIsBillable() {
        return isBillable;
    }

    public void setIsBillable(Boolean billable) {
        isBillable = billable;
    }
}

