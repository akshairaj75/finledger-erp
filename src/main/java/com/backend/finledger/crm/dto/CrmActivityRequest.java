package com.backend.finledger.crm.dto;

import com.backend.finledger.crm.entity.ActivityRelatedType;
import com.backend.finledger.crm.entity.ActivityType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record CrmActivityRequest(
    @NotNull(message = "Activity type is required")
    ActivityType activityType,

    @NotBlank(message = "Subject is required")
    String subject,

    String details,

    @NotNull(message = "Related type is required")
    ActivityRelatedType relatedType,

    @NotNull(message = "Related ID is required")
    Long relatedId,

    Instant dueAt,
    Long assignedUserId
) {}

