package com.backend.finledger.crm.dto;

import com.backend.finledger.crm.entity.StageType;

public record CrmPipelineStageDto(
    Long id,
    Long companyId,
    String name,
    Byte sortOrder,
    Byte probabilityPct,
    StageType stageType
) {}

