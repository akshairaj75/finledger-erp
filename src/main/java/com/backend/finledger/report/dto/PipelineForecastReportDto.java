package com.backend.finledger.report.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record PipelineForecastReportDto(
        LocalDate fromDate,
        LocalDate toDate,
        Long ownerId,
        List<PipelineStageSummaryDto> stages,
        BigDecimal totalPipelineValue,
        BigDecimal totalWeightedValue,
        int totalDeals
) {}

