package com.backend.finledger.crm.entity;

import com.backend.finledger.common.BaseEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "crm_pipeline_stages", uniqueConstraints = {
    @UniqueConstraint(name = "uk_stage", columnNames = {"company_id", "name"})
})
public class CrmPipelineStage extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "company_id", nullable = false)
    private Long companyId;

    @Column(name = "name", length = 50, nullable = false)
    private String name;

    @Column(name = "sort_order", nullable = false)
    private Byte sortOrder;

    @Column(name = "probability_pct", nullable = false)
    private Byte probabilityPct = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "stage_type", length = 20, nullable = false)
    private StageType stageType = StageType.OPEN;

    public CrmPipelineStage() {}

    public CrmPipelineStage(Long companyId, String name, Byte sortOrder, Byte probabilityPct, StageType stageType) {
        this.companyId = companyId;
        this.name = name;
        this.sortOrder = sortOrder;
        this.probabilityPct = probabilityPct;
        this.stageType = stageType != null ? stageType : StageType.OPEN;
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

    public Byte getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Byte sortOrder) {
        this.sortOrder = sortOrder;
    }

    public Byte getProbabilityPct() {
        return probabilityPct;
    }

    public void setProbabilityPct(Byte probabilityPct) {
        this.probabilityPct = probabilityPct;
    }

    public StageType getStageType() {
        return stageType;
    }

    public void setStageType(StageType stageType) {
        this.stageType = stageType;
    }
}

