package com.backend.finledger.crm.dto;

import com.backend.finledger.crm.entity.ActivityRelatedType;
import com.backend.finledger.crm.entity.ActivityType;

import java.time.Instant;

public record CrmActivityDto(
    Long id,
    Long companyId,
    ActivityType activityType,
    String subject,
    String details,
    ActivityRelatedType relatedType,
    Long relatedId,
    Instant dueAt,
    Instant completedAt,
    Long assignedUserId,
    String assignedUserName,
    Instant createdAt
) {}

