package com.backend.finledger.crm.dto;

import jakarta.validation.constraints.NotNull;

public record OpportunityStageRequest(
    @NotNull(message = "Stage ID is required")
    Long stageId,

    String lostReason
) {}

