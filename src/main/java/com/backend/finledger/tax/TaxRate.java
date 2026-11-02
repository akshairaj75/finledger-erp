package com.backend.finledger.tax;

import com.backend.finledger.common.BaseEntity;
import jakarta.persistence.*;

import java.math.BigDecimal;

/**
 * TaxRate entity matching tax_rates table schema.
 */
@Entity
@Table(name = "tax_rates", uniqueConstraints = {
    @UniqueConstraint(name = "uk_tax", columnNames = {"company_id", "code"})
})
public class TaxRate extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "company_id", nullable = false)
    private Long companyId;

    @Column(name = "code", length = 20, nullable = false)
    private String code;

    @Column(name = "name", length = 80, nullable = false)
    private String name;

    @Column(name = "rate", precision = 7, scale = 4, nullable = false)
    private BigDecimal rate;

    @Enumerated(EnumType.STRING)
    @Column(name = "tax_type", length = 20, nullable = false)
    private TaxType taxType = TaxType.STANDARD;

    @Column(name = "sales_account_id")
    private Long salesAccountId;

    @Column(name = "purchase_account_id")
    private Long purchaseAccountId;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    public TaxRate() {}

    public TaxRate(Long companyId, String code, String name, BigDecimal rate, TaxType taxType) {
        this.companyId = companyId;
        this.code = code;
        this.name = name;
        this.rate = rate;
        this.taxType = taxType;
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

    public BigDecimal getRate() {
        return rate;
    }

    public void setRate(BigDecimal rate) {
        this.rate = rate;
    }

    public TaxType getTaxType() {
        return taxType;
    }

    public void setTaxType(TaxType taxType) {
        this.taxType = taxType;
    }

    public Long getSalesAccountId() {
        return salesAccountId;
    }

    public void setSalesAccountId(Long salesAccountId) {
        this.salesAccountId = salesAccountId;
    }

    public Long getPurchaseAccountId() {
        return purchaseAccountId;
    }

    public void setPurchaseAccountId(Long purchaseAccountId) {
        this.purchaseAccountId = purchaseAccountId;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean active) {
        isActive = active;
    }
}

