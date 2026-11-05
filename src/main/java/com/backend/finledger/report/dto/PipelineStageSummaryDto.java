package com.backend.finledger.report.dto;

import java.math.BigDecimal;

public record PipelineStageSummaryDto(
        Long stageId,
        String stageName,
        int dealCount,
        BigDecimal totalValue,
        BigDecimal weightedValue,
        Integer probability
) {}

