package com.backend.finledger.inventory.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "stock_adjustment_lines", indexes = {
    @Index(name = "ix_adjl_adj", columnList = "adjustment_id")
})
public class StockAdjustmentLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "adjustment_id", nullable = false)
    private StockAdjustment adjustment;

    @Column(name = "item_id", nullable = false)
    private Long itemId;

    @Column(name = "system_qty", precision = 18, scale = 4, nullable = false)
    private BigDecimal systemQty = BigDecimal.ZERO;

    @Column(name = "counted_qty", precision = 18, scale = 4, nullable = false)
    private BigDecimal countedQty = BigDecimal.ZERO;

    @Column(name = "difference_qty", precision = 18, scale = 4, nullable = false)
    private BigDecimal differenceQty = BigDecimal.ZERO;

    @Column(name = "unit_cost", precision = 18, scale = 4, nullable = false)
    private BigDecimal unitCost = BigDecimal.ZERO;

    @Column(name = "value_impact", precision = 18, scale = 2, nullable = false)
    private BigDecimal valueImpact = BigDecimal.ZERO;

    public StockAdjustmentLine() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public StockAdjustment getAdjustment() {
        return adjustment;
    }

    public void setAdjustment(StockAdjustment adjustment) {
        this.adjustment = adjustment;
    }

    public Long getItemId() {
        return itemId;
    }

    public void setItemId(Long itemId) {
        this.itemId = itemId;
    }

    public BigDecimal getSystemQty() {
        return systemQty;
    }

    public void setSystemQty(BigDecimal systemQty) {
        this.systemQty = systemQty;
    }

    public BigDecimal getCountedQty() {
        return countedQty;
    }

    public void setCountedQty(BigDecimal countedQty) {
        this.countedQty = countedQty;
    }

    public BigDecimal getDifferenceQty() {
        return differenceQty;
    }

    public void setDifferenceQty(BigDecimal differenceQty) {
        this.differenceQty = differenceQty;
    }

    public BigDecimal getUnitCost() {
        return unitCost;
    }

    public void setUnitCost(BigDecimal unitCost) {
        this.unitCost = unitCost;
    }

    public BigDecimal getValueImpact() {
        return valueImpact;
    }

    public void setValueImpact(BigDecimal valueImpact) {
        this.valueImpact = valueImpact;
    }
}

