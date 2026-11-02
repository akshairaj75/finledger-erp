package com.backend.finledger.banking;

import com.backend.finledger.common.BaseEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "bank_accounts")
public class BankAccount extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "company_id", nullable = false)
    private Long companyId;

    @Column(name = "name", length = 100, nullable = false)
    private String name;

    @Column(name = "bank_name", length = 100)
    private String bankName;

    @Column(name = "account_number", length = 40)
    private String accountNumber;

    @Column(name = "iban", length = 34)
    private String iban;

    @Column(name = "swift_code", length = 11)
    private String swiftCode;

    @Column(name = "currency_code", length = 3, nullable = false)
    private String currencyCode;

    @Column(name = "gl_account_id", nullable = false, unique = true)
    private Long glAccountId;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_kind", length = 10, nullable = false)
    private AccountKind accountKind = AccountKind.BANK;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    public BankAccount() {}

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

    public String getBankName() {
        return bankName;
    }

    public void setBankName(String bankName) {
        this.bankName = bankName;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public String getIban() {
        return iban;
    }

    public void setIban(String iban) {
        this.iban = iban;
    }

    public String getSwiftCode() {
        return swiftCode;
    }

    public void setSwiftCode(String swiftCode) {
        this.swiftCode = swiftCode;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }

    public Long getGlAccountId() {
        return glAccountId;
    }

    public void setGlAccountId(Long glAccountId) {
        this.glAccountId = glAccountId;
    }

    public AccountKind getAccountKind() {
        return accountKind;
    }

    public void setAccountKind(AccountKind accountKind) {
        this.accountKind = accountKind;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean active) {
        isActive = active;
    }
}

