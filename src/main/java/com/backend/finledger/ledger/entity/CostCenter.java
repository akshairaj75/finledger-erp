package com.backend.finledger.ledger.entity;

import com.backend.finledger.common.BaseEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "cost_centers", uniqueConstraints = {
    @UniqueConstraint(name = "uk_cc", columnNames = {"company_id", "code"})
})
public class CostCenter extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "company_id", nullable = false)
    private Long companyId;

    @Column(name = "code", length = 20, nullable = false)
    private String code;

    @Column(name = "name", length = 100, nullable = false)
    private String name;

    @Column(name = "parent_id")
    private Long parentId;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    public CostCenter() {}

    public CostCenter(Long companyId, String code, String name, Long parentId) {
        this.companyId = companyId;
        this.code = code;
        this.name = name;
        this.parentId = parentId;
        this.isActive = true;
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

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
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

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean active) {
        isActive = active;
    }
}

