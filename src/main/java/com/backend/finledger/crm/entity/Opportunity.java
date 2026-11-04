package com.backend.finledger.crm.entity;

import com.backend.finledger.common.BaseEntity;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "opportunities", uniqueConstraints = {
    @UniqueConstraint(name = "uk_opp_no", columnNames = {"company_id", "opportunity_no"})
})
public class Opportunity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "company_id", nullable = false)
    private Long companyId;

    @Column(name = "opportunity_no", length = 30, nullable = false)
    private String opportunityNo;

    @Column(name = "title", length = 200, nullable = false)
    private String title;

    @Column(name = "lead_id")
    private Long leadId;

    @Column(name = "customer_id")
    private Long customerId;

    @Column(name = "contact_id")
    private Long contactId;

    @Column(name = "stage_id", nullable = false)
    private Long stageId;

    @Column(name = "expected_value", precision = 18, scale = 2, nullable = false)
    private BigDecimal expectedValue = BigDecimal.ZERO;

    @Column(name = "currency_code", length = 3, nullable = false)
    private String currencyCode;

    @Column(name = "probability_pct", nullable = false)
    private Byte probabilityPct = 0;

    @Column(name = "expected_close_date")
    private LocalDate expectedCloseDate;

    @Column(name = "owner_user_id")
    private Long ownerUserId;

    @Column(name = "lost_reason", length = 255)
    private String lostReason;

    @Column(name = "closed_at")
    private Instant closedAt;

    public Opportunity() {}

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

    public String getOpportunityNo() {
        return opportunityNo;
    }

    public void setOpportunityNo(String opportunityNo) {
        this.opportunityNo = opportunityNo;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Long getLeadId() {
        return leadId;
    }

    public void setLeadId(Long leadId) {
        this.leadId = leadId;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public Long getContactId() {
        return contactId;
    }

    public void setContactId(Long contactId) {
        this.contactId = contactId;
    }

    public Long getStageId() {
        return stageId;
    }

    public void setStageId(Long stageId) {
        this.stageId = stageId;
    }

    public BigDecimal getExpectedValue() {
        return expectedValue;
    }

    public void setExpectedValue(BigDecimal expectedValue) {
        this.expectedValue = expectedValue != null ? expectedValue : BigDecimal.ZERO;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }

    public Byte getProbabilityPct() {
        return probabilityPct;
    }

    public void setProbabilityPct(Byte probabilityPct) {
        this.probabilityPct = probabilityPct != null ? probabilityPct : 0;
    }

    public LocalDate getExpectedCloseDate() {
        return expectedCloseDate;
    }

    public void setExpectedCloseDate(LocalDate expectedCloseDate) {
        this.expectedCloseDate = expectedCloseDate;
    }

    public Long getOwnerUserId() {
        return ownerUserId;
    }

    public void setOwnerUserId(Long ownerUserId) {
        this.ownerUserId = ownerUserId;
    }

    public String getLostReason() {
        return lostReason;
    }

    public void setLostReason(String lostReason) {
        this.lostReason = lostReason;
    }

    public Instant getClosedAt() {
        return closedAt;
    }

    public void setClosedAt(Instant closedAt) {
        this.closedAt = closedAt;
    }
}

