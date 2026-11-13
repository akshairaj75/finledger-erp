package com.backend.finledger.company.entity;

import com.backend.finledger.common.BaseEntity;
import jakarta.persistence.*;

/**
 * Company entity matching companies table.
 */
@Entity
@Table(name = "companies", uniqueConstraints = {
    @UniqueConstraint(name = "uk_companies_code", columnNames = {"code"})
})
public class Company extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", length = 20, nullable = false)
    private String code;

    @Column(name = "legal_name", length = 200, nullable = false)
    private String legalName;

    @Column(name = "trn", length = 20)
    private String trn;

    @Column(name = "country_code", length = 2, nullable = false)
    private String countryCode;

    @Column(name = "base_currency", length = 3, nullable = false)
    private String baseCurrency;

    @Column(name = "fiscal_year_start_month", nullable = false, columnDefinition = "tinyint")
    private Byte fiscalYearStartMonth = 1;

    @Column(name = "address", length = 500)
    private String address;

    @Column(name = "email", length = 150)
    private String email;

    @Column(name = "phone", length = 30)
    private String phone;

    @Column(name = "logo_path", length = 255)
    private String logoPath;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "allow_negative_stock", nullable = false)
    private Boolean allowNegativeStock = false;

    public Company() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getLegalName() {
        return legalName;
    }

    public void setLegalName(String legalName) {
        this.legalName = legalName;
    }

    public String getTrn() {
        return trn;
    }

    public void setTrn(String trn) {
        this.trn = trn;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }

    public String getBaseCurrency() {
        return baseCurrency;
    }

    public void setBaseCurrency(String baseCurrency) {
        this.baseCurrency = baseCurrency;
    }

    public Byte getFiscalYearStartMonth() {
        return fiscalYearStartMonth;
    }

    public void setFiscalYearStartMonth(Byte fiscalYearStartMonth) {
        this.fiscalYearStartMonth = fiscalYearStartMonth;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
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

    public String getLogoPath() {
        return logoPath;
    }

    public void setLogoPath(String logoPath) {
        this.logoPath = logoPath;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean active) {
        isActive = active;
    }

    public Boolean getAllowNegativeStock() {
        return allowNegativeStock;
    }

    public void setAllowNegativeStock(Boolean allowNegativeStock) {
        this.allowNegativeStock = allowNegativeStock;
    }
}

