package com.backend.finledger.crm.dto;

import com.backend.finledger.crm.entity.StageType;

import java.math.BigDecimal;
import java.util.List;

public record OpportunityStageColumnDto(
    Long stageId,
    String stageName,
    Byte sortOrder,
    Byte probabilityPct,
    StageType stageType,
    BigDecimal totalValue,
    int count,
    List<OpportunityDto> opportunities
) {}

