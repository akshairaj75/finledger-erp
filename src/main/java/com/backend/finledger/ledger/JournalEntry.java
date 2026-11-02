package com.backend.finledger.ledger;

import com.backend.finledger.common.BaseEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "journal_entries", uniqueConstraints = {
    @UniqueConstraint(name = "uk_je_no", columnNames = {"company_id", "entry_no"})
})
public class JournalEntry extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "company_id", nullable = false)
    private Long companyId;

    @Column(name = "entry_no", length = 30, nullable = false)
    private String entryNo;

    @Column(name = "entry_date", nullable = false)
    private LocalDate entryDate;

    @Column(name = "fiscal_period_id", nullable = false)
    private Long fiscalPeriodId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fiscal_period_id", insertable = false, updatable = false)
    private FiscalPeriod fiscalPeriod;

    @Column(name = "reference", length = 100)
    private String reference;

    @Column(name = "narration", length = 500)
    private String narration;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, columnDefinition = "ENUM('MANUAL','INVOICE','BILL','RECEIPT','PAYMENT','CREDIT_NOTE','EXPENSE','REIMBURSEMENT','STOCK_ADJUSTMENT','STOCK_TRANSFER','OPENING','CLOSING')")
    private JournalSourceType sourceType = JournalSourceType.MANUAL;

    @Column(name = "source_id")
    private Long sourceId;

    @Column(name = "currency_code", length = 3, nullable = false)
    private String currencyCode;

    @Column(name = "exchange_rate", precision = 18, scale = 6, nullable = false)
    private BigDecimal exchangeRate = BigDecimal.ONE;

    @Column(name = "total_debit", precision = 18, scale = 2, nullable = false)
    private BigDecimal totalDebit = BigDecimal.ZERO;

    @Column(name = "total_credit", precision = 18, scale = 2, nullable = false)
    private BigDecimal totalCredit = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, columnDefinition = "ENUM('DRAFT','POSTED','VOID')")
    private JournalStatus status = JournalStatus.DRAFT;

    @Column(name = "posted_at")
    private LocalDateTime postedAt;

    @Column(name = "posted_by")
    private Long postedBy;

    @Column(name = "reversal_of_id")
    private Long reversalOfId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reversal_of_id", insertable = false, updatable = false)
    private JournalEntry reversalOf;

    @OneToMany(mappedBy = "journalEntry", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("lineNo ASC")
    private List<JournalLine> lines = new ArrayList<>();

    public JournalEntry() {}

    public void addLine(JournalLine line) {
        lines.add(line);
        line.setJournalEntry(this);
    }

    public void removeLine(JournalLine line) {
        lines.remove(line);
        line.setJournalEntry(null);
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

    public String getEntryNo() {
        return entryNo;
    }

    public void setEntryNo(String entryNo) {
        this.entryNo = entryNo;
    }

    public LocalDate getEntryDate() {
        return entryDate;
    }

    public void setEntryDate(LocalDate entryDate) {
        this.entryDate = entryDate;
    }

    public Long getFiscalPeriodId() {
        return fiscalPeriodId;
    }

    public void setFiscalPeriodId(Long fiscalPeriodId) {
        this.fiscalPeriodId = fiscalPeriodId;
    }

    public FiscalPeriod getFiscalPeriod() {
        return fiscalPeriod;
    }

    public void setFiscalPeriod(FiscalPeriod fiscalPeriod) {
        this.fiscalPeriod = fiscalPeriod;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public String getNarration() {
        return narration;
    }

    public void setNarration(String narration) {
        this.narration = narration;
    }

    public JournalSourceType getSourceType() {
        return sourceType;
    }

    public void setSourceType(JournalSourceType sourceType) {
        this.sourceType = sourceType;
    }

    public Long getSourceId() {
        return sourceId;
    }

    public void setSourceId(Long sourceId) {
        this.sourceId = sourceId;
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

    public BigDecimal getTotalDebit() {
        return totalDebit;
    }

    public void setTotalDebit(BigDecimal totalDebit) {
        this.totalDebit = totalDebit;
    }

    public BigDecimal getTotalCredit() {
        return totalCredit;
    }

    public void setTotalCredit(BigDecimal totalCredit) {
        this.totalCredit = totalCredit;
    }

    public JournalStatus getStatus() {
        return status;
    }

    public void setStatus(JournalStatus status) {
        this.status = status;
    }

    public LocalDateTime getPostedAt() {
        return postedAt;
    }

    public void setPostedAt(LocalDateTime postedAt) {
        this.postedAt = postedAt;
    }

    public Long getPostedBy() {
        return postedBy;
    }

    public void setPostedBy(Long postedBy) {
        this.postedBy = postedBy;
    }

    public Long getReversalOfId() {
        return reversalOfId;
    }

    public void setReversalOfId(Long reversalOfId) {
        this.reversalOfId = reversalOfId;
    }

    public JournalEntry getReversalOf() {
        return reversalOf;
    }

    public void setReversalOf(JournalEntry reversalOf) {
        this.reversalOf = reversalOf;
    }

    public List<JournalLine> getLines() {
        return lines;
    }

    public void setLines(List<JournalLine> lines) {
        this.lines = lines;
    }
}
