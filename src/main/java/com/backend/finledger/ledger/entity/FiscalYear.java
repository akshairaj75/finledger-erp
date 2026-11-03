package com.backend.finledger.ledger.entity;

import com.backend.finledger.common.BaseEntity;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "fiscal_years", uniqueConstraints = {
    @UniqueConstraint(name = "uk_fy", columnNames = {"company_id", "name"})
})
public class FiscalYear extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "company_id", nullable = false)
    private Long companyId;

    @Column(name = "name", length = 20, nullable = false)
    private String name;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, columnDefinition = "ENUM('OPEN','CLOSED')")
    private FiscalYearStatus status = FiscalYearStatus.OPEN;

    @OneToMany(mappedBy = "fiscalYear", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("periodNo ASC")
    private List<FiscalPeriod> periods = new ArrayList<>();

    public FiscalYear() {}

    public FiscalYear(Long companyId, String name, LocalDate startDate, LocalDate endDate) {
        this.companyId = companyId;
        this.name = name;
        this.startDate = startDate;
        this.endDate = endDate;
        this.status = FiscalYearStatus.OPEN;
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public FiscalYearStatus getStatus() {
        return status;
    }

    public void setStatus(FiscalYearStatus status) {
        this.status = status;
    }

    public List<FiscalPeriod> getPeriods() {
        return periods;
    }

    public void setPeriods(List<FiscalPeriod> periods) {
        this.periods = periods;
    }
}

