package com.backend.finledger.currency.entity;

import com.backend.finledger.common.BaseEntity;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * ExchangeRate entity matching exchange_rates table schema.
 */
@Entity
@Table(name = "exchange_rates", uniqueConstraints = {
    @UniqueConstraint(name = "uk_fx", columnNames = {"company_id", "from_currency", "to_currency", "rate_date"})
})
public class ExchangeRate extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "company_id", nullable = false)
    private Long companyId;

    @Column(name = "from_currency", length = 3, nullable = false)
    private String fromCurrency;

    @Column(name = "to_currency", length = 3, nullable = false)
    private String toCurrency;

    @Column(name = "rate_date", nullable = false)
    private LocalDate rateDate;

    @Column(name = "rate", precision = 18, scale = 6, nullable = false)
    private BigDecimal rate;

    public ExchangeRate() {}

    public ExchangeRate(Long companyId, String fromCurrency, String toCurrency, LocalDate rateDate, BigDecimal rate) {
        this.companyId = companyId;
        this.fromCurrency = fromCurrency;
        this.toCurrency = toCurrency;
        this.rateDate = rateDate;
        this.rate = rate;
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

    public String getFromCurrency() {
        return fromCurrency;
    }

    public void setFromCurrency(String fromCurrency) {
        this.fromCurrency = fromCurrency;
    }

    public String getToCurrency() {
        return toCurrency;
    }

    public void setToCurrency(String toCurrency) {
        this.toCurrency = toCurrency;
    }

    public LocalDate getRateDate() {
        return rateDate;
    }

    public void setRateDate(LocalDate rateDate) {
        this.rateDate = rateDate;
    }

    public BigDecimal getRate() {
        return rate;
    }

    public void setRate(BigDecimal rate) {
        this.rate = rate;
    }
}

