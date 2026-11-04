package com.backend.finledger.crm.entity;

import com.backend.finledger.common.BaseEntity;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "quotations", uniqueConstraints = {
    @UniqueConstraint(name = "uk_qt_no", columnNames = {"company_id", "quotation_no"})
})
public class Quotation extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "company_id", nullable = false)
    private Long companyId;

    @Column(name = "quotation_no", length = 30, nullable = false)
    private String quotationNo;

    @Column(name = "opportunity_id")
    private Long opportunityId;

    @Column(name = "customer_id")
    private Long customerId;

    @Column(name = "lead_id")
    private Long leadId;

    @Column(name = "quote_date", nullable = false)
    private LocalDate quoteDate;

    @Column(name = "valid_until", nullable = false)
    private LocalDate validUntil;

    @Column(name = "currency_code", length = 3, nullable = false)
    private String currencyCode;

    @Column(name = "subtotal", precision = 18, scale = 2, nullable = false)
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(name = "tax_total", precision = 18, scale = 2, nullable = false)
    private BigDecimal taxTotal = BigDecimal.ZERO;

    @Column(name = "total", precision = 18, scale = 2, nullable = false)
    private BigDecimal total = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 30, nullable = false)
    private QuotationStatus status = QuotationStatus.DRAFT;

    @Column(name = "invoice_id")
    private Long invoiceId;

    @OneToMany(mappedBy = "quotation", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("lineNo ASC")
    private List<QuotationLine> lines = new ArrayList<>();

    public Quotation() {}

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

    public String getQuotationNo() {
        return quotationNo;
    }

    public void setQuotationNo(String quotationNo) {
        this.quotationNo = quotationNo;
    }

    public Long getOpportunityId() {
        return opportunityId;
    }

    public void setOpportunityId(Long opportunityId) {
        this.opportunityId = opportunityId;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public Long getLeadId() {
        return leadId;
    }

    public void setLeadId(Long leadId) {
        this.leadId = leadId;
    }

    public LocalDate getQuoteDate() {
        return quoteDate;
    }

    public void setQuoteDate(LocalDate quoteDate) {
        this.quoteDate = quoteDate;
    }

    public LocalDate getValidUntil() {
        return validUntil;
    }

    public void setValidUntil(LocalDate validUntil) {
        this.validUntil = validUntil;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal != null ? subtotal : BigDecimal.ZERO;
    }

    public BigDecimal getTaxTotal() {
        return taxTotal;
    }

    public void setTaxTotal(BigDecimal taxTotal) {
        this.taxTotal = taxTotal != null ? taxTotal : BigDecimal.ZERO;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public void setTotal(BigDecimal total) {
        this.total = total != null ? total : BigDecimal.ZERO;
    }

    public QuotationStatus getStatus() {
        return status;
    }

    public void setStatus(QuotationStatus status) {
        this.status = status;
    }

    public Long getInvoiceId() {
        return invoiceId;
    }

    public void setInvoiceId(Long invoiceId) {
        this.invoiceId = invoiceId;
    }

    public List<QuotationLine> getLines() {
        return lines;
    }

    public void setLines(List<QuotationLine> lines) {
        this.lines = lines;
    }

    public void addLine(QuotationLine line) {
        lines.add(line);
        line.setQuotation(this);
    }

    public void removeLine(QuotationLine line) {
        lines.remove(line);
        line.setQuotation(null);
    }
}

