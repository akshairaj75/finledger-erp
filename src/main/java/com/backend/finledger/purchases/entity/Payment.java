package com.backend.finledger.purchases.entity;

import com.backend.finledger.common.BaseEntity;
import com.backend.finledger.sales.entity.PaymentMethod;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "payments", uniqueConstraints = {
    @UniqueConstraint(name = "uk_pay_no", columnNames = {"company_id", "payment_no"})
}, indexes = {
    @Index(name = "ix_pay_vendor", columnList = "vendor_id")
})
public class Payment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "company_id", nullable = false)
    private Long companyId;

    @Column(name = "payment_no", length = 30, nullable = false)
    private String paymentNo;

    @Column(name = "vendor_id", nullable = false)
    private Long vendorId;

    @Column(name = "payment_date", nullable = false)
    private LocalDate paymentDate;

    @Column(name = "bank_account_id", nullable = false)
    private Long bankAccountId;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false)
    private PaymentMethod paymentMethod;

    @Column(name = "reference", length = 60)
    private String reference;

    @Column(name = "currency_code", length = 3, nullable = false)
    private String currencyCode;

    @Column(name = "amount", precision = 18, scale = 2, nullable = false)
    private BigDecimal amount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private PaymentStatus status = PaymentStatus.DRAFT;

    @Column(name = "journal_entry_id")
    private Long journalEntryId;

    @OneToMany(mappedBy = "payment", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PaymentAllocation> allocations = new ArrayList<>();

    @Transient
    private BigDecimal unappliedAmount = BigDecimal.ZERO;

    public Payment() {}

    public void addAllocation(PaymentAllocation allocation) {
        allocations.add(allocation);
        allocation.setPayment(this);
    }

    public void removeAllocation(PaymentAllocation allocation) {
        allocations.remove(allocation);
        allocation.setPayment(null);
    }

    public void clearAllocations() {
        for (PaymentAllocation allocation : new ArrayList<>(allocations)) {
            removeAllocation(allocation);
        }
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

    public String getPaymentNo() {
        return paymentNo;
    }

    public void setPaymentNo(String paymentNo) {
        this.paymentNo = paymentNo;
    }

    public Long getVendorId() {
        return vendorId;
    }

    public void setVendorId(Long vendorId) {
        this.vendorId = vendorId;
    }

    public LocalDate getPaymentDate() {
        return paymentDate;
    }

    public void setPaymentDate(LocalDate paymentDate) {
        this.paymentDate = paymentDate;
    }

    public Long getBankAccountId() {
        return bankAccountId;
    }

    public void setBankAccountId(Long bankAccountId) {
        this.bankAccountId = bankAccountId;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public void setStatus(PaymentStatus status) {
        this.status = status;
    }

    public Long getJournalEntryId() {
        return journalEntryId;
    }

    public void setJournalEntryId(Long journalEntryId) {
        this.journalEntryId = journalEntryId;
    }

    public List<PaymentAllocation> getAllocations() {
        return allocations;
    }

    public void setAllocations(List<PaymentAllocation> allocations) {
        this.allocations = allocations;
    }

    public BigDecimal getUnappliedAmount() {
        return unappliedAmount;
    }

    public void setUnappliedAmount(BigDecimal unappliedAmount) {
        this.unappliedAmount = unappliedAmount;
    }
}

