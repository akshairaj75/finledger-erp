package com.backend.finledger.inventory.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "stock_balances", uniqueConstraints = {
    @UniqueConstraint(name = "uk_stock", columnNames = {"item_id", "warehouse_id"})
})
public class StockBalance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "item_id", nullable = false)
    private Long itemId;

    @Column(name = "warehouse_id", nullable = false)
    private Long warehouseId;

    @Column(name = "qty_on_hand", precision = 18, scale = 4, nullable = false)
    private BigDecimal qtyOnHand = BigDecimal.ZERO;

    @Column(name = "qty_reserved", precision = 18, scale = 4, nullable = false)
    private BigDecimal qtyReserved = BigDecimal.ZERO;

    @Column(name = "avg_cost", precision = 18, scale = 4, nullable = false)
    private BigDecimal avgCost = BigDecimal.ZERO;

    @Column(name = "stock_value", precision = 18, scale = 2, nullable = false)
    private BigDecimal stockValue = BigDecimal.ZERO;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Integer version = 0;

    public StockBalance() {}

    public StockBalance(Long itemId, Long warehouseId) {
        this.itemId = itemId;
        this.warehouseId = warehouseId;
        this.qtyOnHand = BigDecimal.ZERO;
        this.qtyReserved = BigDecimal.ZERO;
        this.avgCost = BigDecimal.ZERO;
        this.stockValue = BigDecimal.ZERO;
        this.updatedAt = Instant.now();
        this.version = 0;
    }

    @PrePersist
    @PreUpdate
    protected void onUpdateTimestamp() {
        this.updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getItemId() {
        return itemId;
    }

    public void setItemId(Long itemId) {
        this.itemId = itemId;
    }

    public Long getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(Long warehouseId) {
        this.warehouseId = warehouseId;
    }

    public BigDecimal getQtyOnHand() {
        return qtyOnHand;
    }

    public void setQtyOnHand(BigDecimal qtyOnHand) {
        this.qtyOnHand = qtyOnHand;
    }

    public BigDecimal getQtyReserved() {
        return qtyReserved;
    }

    public void setQtyReserved(BigDecimal qtyReserved) {
        this.qtyReserved = qtyReserved;
    }

    public BigDecimal getAvgCost() {
        return avgCost;
    }

    public void setAvgCost(BigDecimal avgCost) {
        this.avgCost = avgCost;
    }

    public BigDecimal getStockValue() {
        return stockValue;
    }

    public void setStockValue(BigDecimal stockValue) {
        this.stockValue = stockValue;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }
}

