package com.backend.finledger.inventory.entity;

import com.backend.finledger.common.BaseEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "item_categories", uniqueConstraints = {
    @UniqueConstraint(name = "uk_itemcat", columnNames = {"company_id", "parent_id", "name"})
})
public class ItemCategory extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "company_id", nullable = false)
    private Long companyId;

    @Column(name = "name", length = 100, nullable = false)
    private String name;

    @Column(name = "parent_id")
    private Long parentId;

    @Column(name = "income_account_id")
    private Long incomeAccountId;

    @Column(name = "inventory_account_id")
    private Long inventoryAccountId;

    @Column(name = "cogs_account_id")
    private Long cogsAccountId;

    public ItemCategory() {}

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

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

    public Long getIncomeAccountId() {
        return incomeAccountId;
    }

    public void setIncomeAccountId(Long incomeAccountId) {
        this.incomeAccountId = incomeAccountId;
    }

    public Long getInventoryAccountId() {
        return inventoryAccountId;
    }

    public void setInventoryAccountId(Long inventoryAccountId) {
        this.inventoryAccountId = inventoryAccountId;
    }

    public Long getCogsAccountId() {
        return cogsAccountId;
    }

    public void setCogsAccountId(Long cogsAccountId) {
        this.cogsAccountId = cogsAccountId;
    }
}

