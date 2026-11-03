package com.backend.finledger.purchases.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "payment_allocations", uniqueConstraints = {
    @UniqueConstraint(name = "uk_pa", columnNames = {"payment_id", "bill_id"})
})
public class PaymentAllocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id", nullable = false)
    private Payment payment;

    @Column(name = "bill_id", nullable = false)
    private Long billId;

    @Column(name = "amount", precision = 18, scale = 2, nullable = false)
    private BigDecimal amount = BigDecimal.ZERO;

    public PaymentAllocation() {}

    public PaymentAllocation(Payment payment, Long billId, BigDecimal amount) {
        this.payment = payment;
        this.billId = billId;
        this.amount = amount;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Payment getPayment() {
        return payment;
    }

    public void setPayment(Payment payment) {
        this.payment = payment;
    }

    public Long getBillId() {
        return billId;
    }

    public void setBillId(Long billId) {
        this.billId = billId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
}

