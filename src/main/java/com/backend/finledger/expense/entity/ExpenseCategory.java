package com.backend.finledger.expense.entity;

import com.backend.finledger.common.BaseEntity;
import jakarta.persistence.*;

import java.math.BigDecimal;

/**
 * ExpenseCategory entity matching expense_categories table.
 * Expense types (travel, fuel, meals, office supplies...) mapped to GL accounts, with optional limits.
 */
@Entity
@Table(name = "expense_categories", uniqueConstraints = {
    @UniqueConstraint(name = "uk_expcat", columnNames = {"company_id", "name"})
})
public class ExpenseCategory extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "company_id", nullable = false)
    private Long companyId;

    @Column(name = "name", length = 100, nullable = false)
    private String name;

    @Column(name = "account_id", nullable = false)
    private Long accountId;

    @Column(name = "default_tax_rate_id")
    private Long defaultTaxRateId;

    @Column(name = "limit_per_claim", precision = 18, scale = 2)
    private BigDecimal limitPerClaim;

    @Column(name = "receipt_required", nullable = false)
    private Boolean receiptRequired = true;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    public ExpenseCategory() {}

    public ExpenseCategory(Long companyId, String name, Long accountId, Long defaultTaxRateId,
                           BigDecimal limitPerClaim, Boolean receiptRequired, Boolean isActive) {
        this.companyId = companyId;
        this.name = name;
        this.accountId = accountId;
        this.defaultTaxRateId = defaultTaxRateId;
        this.limitPerClaim = limitPerClaim;
        this.receiptRequired = receiptRequired != null ? receiptRequired : true;
        this.isActive = isActive != null ? isActive : true;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getCompanyId() {
        return companyId;
    }

    public void setCompanyId(Long companyId) {
        this.companyId = companyId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    public Long getDefaultTaxRateId() {
        return defaultTaxRateId;
    }

    public void setDefaultTaxRateId(Long defaultTaxRateId) {
        this.defaultTaxRateId = defaultTaxRateId;
    }

    public BigDecimal getLimitPerClaim() {
        return limitPerClaim;
    }

    public void setLimitPerClaim(BigDecimal limitPerClaim) {
        this.limitPerClaim = limitPerClaim;
    }

    public Boolean getReceiptRequired() {
        return receiptRequired;
    }

    public void setReceiptRequired(Boolean receiptRequired) {
        this.receiptRequired = receiptRequired;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean active) {
        isActive = active;
    }
}

