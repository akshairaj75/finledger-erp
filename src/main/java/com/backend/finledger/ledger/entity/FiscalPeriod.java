package com.backend.finledger.ledger.entity;

import com.backend.finledger.common.BaseEntity;
import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "fiscal_periods", uniqueConstraints = {
    @UniqueConstraint(name = "uk_period", columnNames = {"fiscal_year_id", "period_no"})
})
public class FiscalPeriod extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "fiscal_year_id", nullable = false)
    private Long fiscalYearId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fiscal_year_id", insertable = false, updatable = false)
    private FiscalYear fiscalYear;

    @Column(name = "period_no", nullable = false, columnDefinition = "tinyint")
    private Byte periodNo;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, columnDefinition = "ENUM('OPEN','CLOSED','LOCKED')")
    private FiscalPeriodStatus status = FiscalPeriodStatus.OPEN;

    public FiscalPeriod() {}

    public FiscalPeriod(Long fiscalYearId, Byte periodNo, LocalDate startDate, LocalDate endDate) {
        this.fiscalYearId = fiscalYearId;
        this.periodNo = periodNo;
        this.startDate = startDate;
        this.endDate = endDate;
        this.status = FiscalPeriodStatus.OPEN;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getFiscalYearId() {
        return fiscalYearId;
    }

    public void setFiscalYearId(Long fiscalYearId) {
        this.fiscalYearId = fiscalYearId;
    }

    public FiscalYear getFiscalYear() {
        return fiscalYear;
    }

    public void setFiscalYear(FiscalYear fiscalYear) {
        this.fiscalYear = fiscalYear;
    }

    public Byte getPeriodNo() {
        return periodNo;
    }

    public void setPeriodNo(Byte periodNo) {
        this.periodNo = periodNo;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public FiscalPeriodStatus getStatus() {
        return status;
    }

    public void setStatus(FiscalPeriodStatus status) {
        this.status = status;
    }
}

