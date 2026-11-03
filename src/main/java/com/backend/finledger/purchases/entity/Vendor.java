package com.backend.finledger.purchases.entity;

import com.backend.finledger.common.BaseEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "vendors", uniqueConstraints = {
    @UniqueConstraint(name = "uk_vendors", columnNames = {"company_id", "code"})
})
public class Vendor extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "company_id", nullable = false)
    private Long companyId;

    @Column(name = "code", length = 20, nullable = false)
    private String code;

    @Column(name = "name", length = 200, nullable = false)
    private String name;

    @Column(name = "trn", length = 20)
    private String trn;

    @Column(name = "email", length = 150)
    private String email;

    @Column(name = "phone", length = 30)
    private String phone;

    @Column(name = "address", length = 500)
    private String address;

    @Column(name = "currency_code", length = 3, nullable = false)
    private String currencyCode;

    @Column(name = "payment_terms_days", nullable = false)
    private Short paymentTermsDays = 30;

    @Column(name = "bank_iban", length = 34)
    private String bankIban;

    @Column(name = "ap_account_id", nullable = false)
    private Long apAccountId;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    public Vendor() {}

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

    public String getTrn() {
        return trn;
    }

    public void setTrn(String trn) {
        this.trn = trn;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }

    public Short getPaymentTermsDays() {
        return paymentTermsDays;
    }

    public void setPaymentTermsDays(Short paymentTermsDays) {
        this.paymentTermsDays = paymentTermsDays;
    }

    public String getBankIban() {
        return bankIban;
    }

    public void setBankIban(String bankIban) {
        this.bankIban = bankIban;
    }

    public Long getApAccountId() {
        return apAccountId;
    }

    public void setApAccountId(Long apAccountId) {
        this.apAccountId = apAccountId;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
}

