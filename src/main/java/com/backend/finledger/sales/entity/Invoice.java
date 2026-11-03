package com.backend.finledger.sales.entity;

import com.backend.finledger.common.BaseEntity;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "invoices", uniqueConstraints = {
    @UniqueConstraint(name = "uk_inv_no", columnNames = {"company_id", "invoice_no"})
})
public class Invoice extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "company_id", nullable = false)
    private Long companyId;

    @Column(name = "invoice_no", length = 30, nullable = false)
    private String invoiceNo;

    @Enumerated(EnumType.STRING)
    @Column(name = "doc_type", length = 20, nullable = false)
    private InvoiceDocType docType = InvoiceDocType.INVOICE;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "invoice_date", nullable = false)
    private LocalDate invoiceDate;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(name = "currency_code", length = 3, nullable = false)
    private String currencyCode;

    @Column(name = "exchange_rate", precision = 18, scale = 6, nullable = false)
    private BigDecimal exchangeRate = BigDecimal.ONE.setScale(6);

    @Column(name = "subtotal", precision = 18, scale = 2, nullable = false)
    private BigDecimal subtotal = BigDecimal.ZERO.setScale(2);

    @Column(name = "discount_total", precision = 18, scale = 2, nullable = false)
    private BigDecimal discountTotal = BigDecimal.ZERO.setScale(2);

    @Column(name = "tax_total", precision = 18, scale = 2, nullable = false)
    private BigDecimal taxTotal = BigDecimal.ZERO.setScale(2);

    @Column(name = "total", precision = 18, scale = 2, nullable = false)
    private BigDecimal total = BigDecimal.ZERO.setScale(2);

    @Column(name = "amount_paid", precision = 18, scale = 2, nullable = false)
    private BigDecimal amountPaid = BigDecimal.ZERO.setScale(2);

    @Column(name = "balance_due", precision = 18, scale = 2, nullable = false)
    private BigDecimal balanceDue = BigDecimal.ZERO.setScale(2);

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private InvoiceStatus status = InvoiceStatus.DRAFT;

    @Column(name = "notes", length = 1000)
    private String notes;

    @Column(name = "quotation_id")
    private Long quotationId;

    @Column(name = "salesperson_user_id")
    private Long salespersonUserId;

    @Column(name = "journal_entry_id")
    private Long journalEntryId;

    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("lineNo ASC")
    private List<InvoiceLine> lines = new ArrayList<>();

    public Invoice() {}

    public void addLine(InvoiceLine line) {
        lines.add(line);
        line.setInvoice(this);
    }

    public void removeLine(InvoiceLine line) {
        lines.remove(line);
        line.setInvoice(null);
    }

    public void clearLines() {
        for (InvoiceLine line : lines) {
            line.setInvoice(null);
        }
        lines.clear();
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

    public String getInvoiceNo() {
        return invoiceNo;
    }

    public void setInvoiceNo(String invoiceNo) {
        this.invoiceNo = invoiceNo;
    }

    public InvoiceDocType getDocType() {
        return docType;
    }

    public void setDocType(InvoiceDocType docType) {
        this.docType = docType;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public LocalDate getInvoiceDate() {
        return invoiceDate;
    }

    public void setInvoiceDate(LocalDate invoiceDate) {
        this.invoiceDate = invoiceDate;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }

    public BigDecimal getExchangeRate() {
        return exchangeRate;
    }

    public void setExchangeRate(BigDecimal exchangeRate) {
        this.exchangeRate = exchangeRate;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
    }

    public BigDecimal getDiscountTotal() {
        return discountTotal;
    }

    public void setDiscountTotal(BigDecimal discountTotal) {
        this.discountTotal = discountTotal;
    }

    public BigDecimal getTaxTotal() {
        return taxTotal;
    }

    public void setTaxTotal(BigDecimal taxTotal) {
        this.taxTotal = taxTotal;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }

    public BigDecimal getAmountPaid() {
        return amountPaid;
    }

    public void setAmountPaid(BigDecimal amountPaid) {
        this.amountPaid = amountPaid;
    }

    public BigDecimal getBalanceDue() {
        return balanceDue;
    }

    public void setBalanceDue(BigDecimal balanceDue) {
        this.balanceDue = balanceDue;
    }

    public InvoiceStatus getStatus() {
        return status;
    }

    public void setStatus(InvoiceStatus status) {
        this.status = status;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Long getQuotationId() {
        return quotationId;
    }

    public void setQuotationId(Long quotationId) {
        this.quotationId = quotationId;
    }

    public Long getSalespersonUserId() {
        return salespersonUserId;
    }

    public void setSalespersonUserId(Long salespersonUserId) {
        this.salespersonUserId = salespersonUserId;
    }

    public Long getJournalEntryId() {
        return journalEntryId;
    }

    public void setJournalEntryId(Long journalEntryId) {
        this.journalEntryId = journalEntryId;
    }

    public List<InvoiceLine> getLines() {
        return lines;
    }

    public void setLines(List<InvoiceLine> lines) {
        this.lines = lines;
    }
}

