package com.backend.finledger.crm.dto;

import com.backend.finledger.crm.entity.StageType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CrmPipelineStageRequest(
    @NotBlank(message = "Stage name is required")
    String name,

    @NotNull(message = "Sort order is required")
    Byte sortOrder,

    Byte probabilityPct,

    @NotNull(message = "Stage type is required")
    StageType stageType
) {}

