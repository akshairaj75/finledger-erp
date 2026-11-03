package com.backend.finledger.ledger.entity;

import com.backend.finledger.common.BaseEntity;
import jakarta.persistence.*;

import java.math.BigDecimal;

/**
 * Account entity matching accounts table schema (Chart of Accounts).
 */
@Entity
@Table(name = "accounts", uniqueConstraints = {
    @UniqueConstraint(name = "uk_accounts", columnNames = {"company_id", "code"})
}, indexes = {
    @Index(name = "ix_accounts_parent", columnList = "parent_id"),
    @Index(name = "ix_accounts_type", columnList = "company_id, account_type")
})
public class Account extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "company_id", nullable = false)
    private Long companyId;

    @Column(name = "code", length = 20, nullable = false)
    private String code;

    @Column(name = "name", length = 150, nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", length = 20, nullable = false)
    private AccountType accountType;

    @Column(name = "sub_type", length = 40)
    private String subType;

    @Column(name = "parent_id")
    private Long parentId;

    @Column(name = "is_group", nullable = false)
    private Boolean isGroup = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "normal_balance", length = 10, nullable = false)
    private NormalBalance normalBalance;

    @Column(name = "currency_code", length = 3)
    private String currencyCode;

    @Column(name = "opening_balance", precision = 18, scale = 2, nullable = false)
    private BigDecimal openingBalance = BigDecimal.ZERO;

    @Column(name = "is_system", nullable = false)
    private Boolean isSystem = false;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    public Account() {}

    public Account(Long companyId, String code, String name, AccountType accountType,
                   String subType, Long parentId, Boolean isGroup, NormalBalance normalBalance) {
        this.companyId = companyId;
        this.code = code;
        this.name = name;
        this.accountType = accountType;
        this.subType = subType;
        this.parentId = parentId;
        this.isGroup = isGroup;
        this.normalBalance = normalBalance;
        this.openingBalance = BigDecimal.ZERO;
        this.isSystem = false;
        this.isActive = true;
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

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public AccountType getAccountType() {
        return accountType;
    }

    public void setAccountType(AccountType accountType) {
        this.accountType = accountType;
    }

    public String getSubType() {
        return subType;
    }

    public void setSubType(String subType) {
        this.subType = subType;
    }

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

    public Boolean getIsGroup() {
        return isGroup;
    }

    public void setIsGroup(Boolean group) {
        isGroup = group;
    }

    public NormalBalance getNormalBalance() {
        return normalBalance;
    }

    public void setNormalBalance(NormalBalance normalBalance) {
        this.normalBalance = normalBalance;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }

    public BigDecimal getOpeningBalance() {
        return openingBalance;
    }

    public void setOpeningBalance(BigDecimal openingBalance) {
        this.openingBalance = openingBalance;
    }

    public Boolean getIsSystem() {
        return isSystem;
    }

    public void setIsSystem(Boolean system) {
        isSystem = system;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean active) {
        isActive = active;
    }
}

