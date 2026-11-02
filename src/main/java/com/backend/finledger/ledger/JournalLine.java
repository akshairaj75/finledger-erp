package com.backend.finledger.ledger;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "journal_lines")
public class JournalLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "journal_entry_id", nullable = false)
    private JournalEntry journalEntry;

    @Column(name = "line_no", nullable = false)
    private Short lineNo;

    @Column(name = "account_id", nullable = false)
    private Long accountId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", insertable = false, updatable = false)
    private Account account;

    @Column(name = "cost_center_id")
    private Long costCenterId;

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "debit", precision = 18, scale = 2, nullable = false)
    private BigDecimal debit = BigDecimal.ZERO;

    @Column(name = "credit", precision = 18, scale = 2, nullable = false)
    private BigDecimal credit = BigDecimal.ZERO;

    @Column(name = "base_debit", precision = 18, scale = 2, nullable = false)
    private BigDecimal baseDebit = BigDecimal.ZERO;

    @Column(name = "base_credit", precision = 18, scale = 2, nullable = false)
    private BigDecimal baseCredit = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "party_type", columnDefinition = "ENUM('CUSTOMER','VENDOR')")
    private PartyType partyType;

    @Column(name = "party_id")
    private Long partyId;

    @Column(name = "is_reconciled", nullable = false)
    private Boolean isReconciled = false;

    public JournalLine() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public JournalEntry getJournalEntry() {
        return journalEntry;
    }

    public void setJournalEntry(JournalEntry journalEntry) {
        this.journalEntry = journalEntry;
    }

    public Short getLineNo() {
        return lineNo;
    }

    public void setLineNo(Short lineNo) {
        this.lineNo = lineNo;
    }

    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    public Account getAccount() {
        return account;
    }

    public void setAccount(Account account) {
        this.account = account;
    }

    public Long getCostCenterId() {
        return costCenterId;
    }

    public void setCostCenterId(Long costCenterId) {
        this.costCenterId = costCenterId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getDebit() {
        return debit;
    }

    public void setDebit(BigDecimal debit) {
        this.debit = debit != null ? debit : BigDecimal.ZERO;
    }

    public BigDecimal getCredit() {
        return credit;
    }

    public void setCredit(BigDecimal credit) {
        this.credit = credit != null ? credit : BigDecimal.ZERO;
    }

    public BigDecimal getBaseDebit() {
        return baseDebit;
    }

    public void setBaseDebit(BigDecimal baseDebit) {
        this.baseDebit = baseDebit != null ? baseDebit : BigDecimal.ZERO;
    }

    public BigDecimal getBaseCredit() {
        return baseCredit;
    }

    public void setBaseCredit(BigDecimal baseCredit) {
        this.baseCredit = baseCredit != null ? baseCredit : BigDecimal.ZERO;
    }

    public PartyType getPartyType() {
        return partyType;
    }

    public void setPartyType(PartyType partyType) {
        this.partyType = partyType;
    }

    public Long getPartyId() {
        return partyId;
    }

    public void setPartyId(Long partyId) {
        this.partyId = partyId;
    }

    public Boolean getIsReconciled() {
        return isReconciled;
    }

    public void setIsReconciled(Boolean reconciled) {
        isReconciled = reconciled;
    }
}
