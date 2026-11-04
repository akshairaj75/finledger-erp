package com.backend.finledger.crm.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "quotation_lines", indexes = {
    @Index(name = "ix_ql_quote", columnList = "quotation_id")
})
public class QuotationLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quotation_id", nullable = false)
    @JsonIgnore
    private Quotation quotation;

    @Column(name = "line_no", nullable = false)
    private Short lineNo;

    @Column(name = "item_id")
    private Long itemId;

    @Column(name = "description", length = 255, nullable = false)
    private String description;

    @Column(name = "quantity", precision = 18, scale = 4, nullable = false)
    private BigDecimal quantity = BigDecimal.ONE;

    @Column(name = "unit_price", precision = 18, scale = 2, nullable = false)
    private BigDecimal unitPrice = BigDecimal.ZERO;

    @Column(name = "discount_pct", precision = 5, scale = 2, nullable = false)
    private BigDecimal discountPct = BigDecimal.ZERO;

    @Column(name = "tax_rate_id")
    private Long taxRateId;

    @Column(name = "line_total", precision = 18, scale = 2, nullable = false)
    private BigDecimal lineTotal = BigDecimal.ZERO;

    public QuotationLine() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Quotation getQuotation() {
        return quotation;
    }

    public void setQuotation(Quotation quotation) {
        this.quotation = quotation;
    }

    public Short getLineNo() {
        return lineNo;
    }

    public void setLineNo(Short lineNo) {
        this.lineNo = lineNo;
    }

    public Long getItemId() {
        return itemId;
    }

    public void setItemId(Long itemId) {
        this.itemId = itemId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity != null ? quantity : BigDecimal.ONE;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice != null ? unitPrice : BigDecimal.ZERO;
    }

    public BigDecimal getDiscountPct() {
        return discountPct;
    }

    public void setDiscountPct(BigDecimal discountPct) {
        this.discountPct = discountPct != null ? discountPct : BigDecimal.ZERO;
    }

    public Long getTaxRateId() {
        return taxRateId;
    }

    public void setTaxRateId(Long taxRateId) {
        this.taxRateId = taxRateId;
    }

    public BigDecimal getLineTotal() {
        return lineTotal;
    }

    public void setLineTotal(BigDecimal lineTotal) {
        this.lineTotal = lineTotal != null ? lineTotal : BigDecimal.ZERO;
    }
}

