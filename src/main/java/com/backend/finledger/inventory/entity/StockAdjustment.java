package com.backend.finledger.inventory.entity;

import com.backend.finledger.common.BaseEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "stock_adjustments", uniqueConstraints = {
    @UniqueConstraint(name = "uk_adj_no", columnNames = {"company_id", "adjustment_no"})
})
public class StockAdjustment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "company_id", nullable = false)
    private Long companyId;

    @Column(name = "adjustment_no", length = 30, nullable = false)
    private String adjustmentNo;

    @Column(name = "adjustment_date", nullable = false)
    private LocalDate adjustmentDate;

    @Column(name = "warehouse_id", nullable = false)
    private Long warehouseId;

    @Enumerated(EnumType.STRING)
    @Column(name = "reason", nullable = false)
    private AdjustmentReason reason = AdjustmentReason.STOCK_COUNT;

    @Column(name = "adjustment_account_id", nullable = false)
    private Long adjustmentAccountId;

    @Column(name = "notes", length = 500)
    private String notes;

    @Column(name = "total_value", precision = 18, scale = 2, nullable = false)
    private BigDecimal totalValue = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private AdjustmentStatus status = AdjustmentStatus.DRAFT;

    @Column(name = "journal_entry_id")
    private Long journalEntryId;

    @OneToMany(mappedBy = "adjustment", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<StockAdjustmentLine> lines = new ArrayList<>();

    public StockAdjustment() {}

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

    public String getAdjustmentNo() {
        return adjustmentNo;
    }

    public void setAdjustmentNo(String adjustmentNo) {
        this.adjustmentNo = adjustmentNo;
    }

    public LocalDate getAdjustmentDate() {
        return adjustmentDate;
    }

    public void setAdjustmentDate(LocalDate adjustmentDate) {
        this.adjustmentDate = adjustmentDate;
    }

    public Long getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(Long warehouseId) {
        this.warehouseId = warehouseId;
    }

    public AdjustmentReason getReason() {
        return reason;
    }

    public void setReason(AdjustmentReason reason) {
        this.reason = reason;
    }

    public Long getAdjustmentAccountId() {
        return adjustmentAccountId;
    }

    public void setAdjustmentAccountId(Long adjustmentAccountId) {
        this.adjustmentAccountId = adjustmentAccountId;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public BigDecimal getTotalValue() {
        return totalValue;
    }

    public void setTotalValue(BigDecimal totalValue) {
        this.totalValue = totalValue;
    }

    public AdjustmentStatus getStatus() {
        return status;
    }

    public void setStatus(AdjustmentStatus status) {
        this.status = status;
    }

    public Long getJournalEntryId() {
        return journalEntryId;
    }

    public void setJournalEntryId(Long journalEntryId) {
        this.journalEntryId = journalEntryId;
    }

    public List<StockAdjustmentLine> getLines() {
        return lines;
    }

    public void setLines(List<StockAdjustmentLine> lines) {
        this.lines = lines;
    }

    public void addLine(StockAdjustmentLine line) {
        lines.add(line);
        line.setAdjustment(this);
    }
}

