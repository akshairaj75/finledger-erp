package com.backend.finledger.expense.entity;

import com.backend.finledger.common.BaseEntity;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Expense entity matching expenses table.
 * Expense header — either paid directly by the company or an employee claim to be reimbursed.
 */
@Entity
@Table(name = "expenses", uniqueConstraints = {
    @UniqueConstraint(name = "uk_exp_no", columnNames = {"company_id", "expense_no"})
}, indexes = {
    @Index(name = "ix_exp_status", columnList = "company_id, status"),
    @Index(name = "ix_exp_employee", columnList = "employee_user_id")
})
public class Expense extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "company_id", nullable = false)
    private Long companyId;

    @Column(name = "expense_no", length = 30, nullable = false)
    private String expenseNo;

    @Column(name = "expense_date", nullable = false)
    private LocalDate expenseDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "paid_by", length = 20, nullable = false)
    private PaidBy paidBy;

    @Column(name = "employee_user_id")
    private Long employeeUserId;

    @Column(name = "vendor_id")
    private Long vendorId;

    @Column(name = "payee_name", length = 150)
    private String payeeName;

    @Column(name = "bank_account_id")
    private Long bankAccountId;

    @Column(name = "currency_code", length = 3, nullable = false)
    private String currencyCode;

    @Column(name = "exchange_rate", precision = 18, scale = 6, nullable = false)
    private BigDecimal exchangeRate = BigDecimal.ONE;

    @Column(name = "subtotal", precision = 18, scale = 2, nullable = false)
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(name = "tax_total", precision = 18, scale = 2, nullable = false)
    private BigDecimal taxTotal = BigDecimal.ZERO;

    @Column(name = "total", precision = 18, scale = 2, nullable = false)
    private BigDecimal total = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private ExpenseStatus status = ExpenseStatus.DRAFT;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @Column(name = "approved_by")
    private Long approvedBy;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @Column(name = "rejection_reason", length = 255)
    private String rejectionReason;

    @Column(name = "journal_entry_id")
    private Long journalEntryId;

    @Column(name = "reimbursement_entry_id")
    private Long reimbursementEntryId;

    @OneToMany(mappedBy = "expense", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ExpenseLine> lines = new ArrayList<>();

    public Expense() {}

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

    public String getExpenseNo() {
        return expenseNo;
    }

    public void setExpenseNo(String expenseNo) {
        this.expenseNo = expenseNo;
    }

    public LocalDate getExpenseDate() {
        return expenseDate;
    }

    public void setExpenseDate(LocalDate expenseDate) {
        this.expenseDate = expenseDate;
    }

    public PaidBy getPaidBy() {
        return paidBy;
    }

    public void setPaidBy(PaidBy paidBy) {
        this.paidBy = paidBy;
    }

    public Long getEmployeeUserId() {
        return employeeUserId;
    }

    public void setEmployeeUserId(Long employeeUserId) {
        this.employeeUserId = employeeUserId;
    }

    public Long getVendorId() {
        return vendorId;
    }

    public void setVendorId(Long vendorId) {
        this.vendorId = vendorId;
    }

    public String getPayeeName() {
        return payeeName;
    }

    public void setPayeeName(String payeeName) {
        this.payeeName = payeeName;
    }

    public Long getBankAccountId() {
        return bankAccountId;
    }

    public void setBankAccountId(Long bankAccountId) {
        this.bankAccountId = bankAccountId;
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

    public ExpenseStatus getStatus() {
        return status;
    }

    public void setStatus(ExpenseStatus status) {
        this.status = status;
    }

    public Instant getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(Instant submittedAt) {
        this.submittedAt = submittedAt;
    }

    public Long getApprovedBy() {
        return approvedBy;
    }

    public void setApprovedBy(Long approvedBy) {
        this.approvedBy = approvedBy;
    }

    public Instant getApprovedAt() {
        return approvedAt;
    }

    public void setApprovedAt(Instant approvedAt) {
        this.approvedAt = approvedAt;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public Long getJournalEntryId() {
        return journalEntryId;
    }

    public void setJournalEntryId(Long journalEntryId) {
        this.journalEntryId = journalEntryId;
    }

    public Long getReimbursementEntryId() {
        return reimbursementEntryId;
    }

    public void setReimbursementEntryId(Long reimbursementEntryId) {
        this.reimbursementEntryId = reimbursementEntryId;
    }

    public List<ExpenseLine> getLines() {
        return lines;
    }

    public void setLines(List<ExpenseLine> lines) {
        this.lines.clear();
        if (lines != null) {
            lines.forEach(this::addLine);
        }
    }

    public void addLine(ExpenseLine line) {
        this.lines.add(line);
        line.setExpense(this);
    }

    public void removeLine(ExpenseLine line) {
        this.lines.remove(line);
        line.setExpense(null);
    }
}

