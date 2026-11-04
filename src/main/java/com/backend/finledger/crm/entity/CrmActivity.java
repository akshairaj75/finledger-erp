package com.backend.finledger.crm.entity;

import com.backend.finledger.common.BaseEntity;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "crm_activities", indexes = {
    @Index(name = "ix_act_related", columnList = "related_type, related_id"),
    @Index(name = "ix_act_due", columnList = "assigned_user_id, due_at")
})
public class CrmActivity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "company_id", nullable = false)
    private Long companyId;

    @Enumerated(EnumType.STRING)
    @Column(name = "activity_type", length = 30, nullable = false)
    private ActivityType activityType;

    @Column(name = "subject", length = 200, nullable = false)
    private String subject;

    @Column(name = "details", columnDefinition = "TEXT")
    private String details;

    @Enumerated(EnumType.STRING)
    @Column(name = "related_type", length = 30, nullable = false)
    private ActivityRelatedType relatedType;

    @Column(name = "related_id", nullable = false)
    private Long relatedId;

    @Column(name = "due_at")
    private Instant dueAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "assigned_user_id")
    private Long assignedUserId;

    public CrmActivity() {}

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

    public ActivityType getActivityType() {
        return activityType;
    }

    public void setActivityType(ActivityType activityType) {
        this.activityType = activityType;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public ActivityRelatedType getRelatedType() {
        return relatedType;
    }

    public void setRelatedType(ActivityRelatedType relatedType) {
        this.relatedType = relatedType;
    }

    public Long getRelatedId() {
        return relatedId;
    }

    public void setRelatedId(Long relatedId) {
        this.relatedId = relatedId;
    }

    public Instant getDueAt() {
        return dueAt;
    }

    public void setDueAt(Instant dueAt) {
        this.dueAt = dueAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }

    public Long getAssignedUserId() {
        return assignedUserId;
    }

    public void setAssignedUserId(Long assignedUserId) {
        this.assignedUserId = assignedUserId;
    }
}

