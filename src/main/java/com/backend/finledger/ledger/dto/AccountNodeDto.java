package com.backend.finledger.ledger.dto;

import com.backend.finledger.ledger.AccountType;
import com.backend.finledger.ledger.NormalBalance;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class AccountNodeDto {
    private Long id;
    private Long companyId;
    private String code;
    private String name;
    private AccountType accountType;
    private String subType;
    private Long parentId;
    private Boolean isGroup;
    private NormalBalance normalBalance;
    private String currencyCode;
    private BigDecimal openingBalance;
    private Boolean isSystem;
    private Boolean isActive;
    private List<AccountNodeDto> children = new ArrayList<>();

    public AccountNodeDto() {}

    public AccountNodeDto(AccountDto dto) {
        this.id = dto.id();
        this.companyId = dto.companyId();
        this.code = dto.code();
        this.name = dto.name();
        this.accountType = dto.accountType();
        this.subType = dto.subType();
        this.parentId = dto.parentId();
        this.isGroup = dto.isGroup();
        this.normalBalance = dto.normalBalance();
        this.currencyCode = dto.currencyCode();
        this.openingBalance = dto.openingBalance();
        this.isSystem = dto.isSystem();
        this.isActive = dto.isActive();
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

    public List<AccountNodeDto> getChildren() {
        return children;
    }

    public void setChildren(List<AccountNodeDto> children) {
        this.children = children;
    }
}

