package com.backend.finledger.inventory.entity;

import com.backend.finledger.common.BaseEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "items", uniqueConstraints = {
    @UniqueConstraint(name = "uk_items", columnNames = {"company_id", "sku"})
})
public class Item extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "company_id", nullable = false)
    private Long companyId;

    @Column(name = "sku", length = 40, nullable = false)
    private String sku;

    @Column(name = "barcode", length = 40)
    private String barcode;

    @Column(name = "name", length = 150, nullable = false)
    private String name;

    @Column(name = "category_id")
    private Long categoryId;

    @Enumerated(EnumType.STRING)
    @Column(name = "item_type", nullable = false)
    private ItemType itemType = ItemType.INVENTORY;

    @Column(name = "unit", length = 20, nullable = false)
    private String unit = "PCS";

    @Enumerated(EnumType.STRING)
    @Column(name = "costing_method", nullable = false)
    private CostingMethod costingMethod = CostingMethod.WEIGHTED_AVERAGE;

    @Column(name = "sales_price", precision = 18, scale = 2, nullable = false)
    private BigDecimal salesPrice = BigDecimal.ZERO;

    @Column(name = "purchase_price", precision = 18, scale = 2, nullable = false)
    private BigDecimal purchasePrice = BigDecimal.ZERO;

    @Column(name = "reorder_level", precision = 18, scale = 4, nullable = false)
    private BigDecimal reorderLevel = BigDecimal.ZERO;

    @Column(name = "reorder_qty", precision = 18, scale = 4, nullable = false)
    private BigDecimal reorderQty = BigDecimal.ZERO;

    @Column(name = "income_account_id")
    private Long incomeAccountId;

    @Column(name = "cogs_account_id")
    private Long cogsAccountId;

    @Column(name = "inventory_account_id")
    private Long inventoryAccountId;

    @Column(name = "tax_rate_id")
    private Long taxRateId;

    @Column(name = "image_path", length = 255)
    private String imagePath;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    public Item() {}

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

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public String getBarcode() {
        return barcode;
    }

    public void setBarcode(String barcode) {
        this.barcode = barcode;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public ItemType getItemType() {
        return itemType;
    }

    public void setItemType(ItemType itemType) {
        this.itemType = itemType;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public CostingMethod getCostingMethod() {
        return costingMethod;
    }

    public void setCostingMethod(CostingMethod costingMethod) {
        this.costingMethod = costingMethod;
    }

    public BigDecimal getSalesPrice() {
        return salesPrice;
    }

    public void setSalesPrice(BigDecimal salesPrice) {
        this.salesPrice = salesPrice;
    }

    public BigDecimal getPurchasePrice() {
        return purchasePrice;
    }

    public void setPurchasePrice(BigDecimal purchasePrice) {
        this.purchasePrice = purchasePrice;
    }

    public BigDecimal getReorderLevel() {
        return reorderLevel;
    }

    public void setReorderLevel(BigDecimal reorderLevel) {
        this.reorderLevel = reorderLevel;
    }

    public BigDecimal getReorderQty() {
        return reorderQty;
    }

    public void setReorderQty(BigDecimal reorderQty) {
        this.reorderQty = reorderQty;
    }

    public Long getIncomeAccountId() {
        return incomeAccountId;
    }

    public void setIncomeAccountId(Long incomeAccountId) {
        this.incomeAccountId = incomeAccountId;
    }

    public Long getCogsAccountId() {
        return cogsAccountId;
    }

    public void setCogsAccountId(Long cogsAccountId) {
        this.cogsAccountId = cogsAccountId;
    }

    public Long getInventoryAccountId() {
        return inventoryAccountId;
    }

    public void setInventoryAccountId(Long inventoryAccountId) {
        this.inventoryAccountId = inventoryAccountId;
    }

    public Long getTaxRateId() {
        return taxRateId;
    }

    public void setTaxRateId(Long taxRateId) {
        this.taxRateId = taxRateId;
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
}

