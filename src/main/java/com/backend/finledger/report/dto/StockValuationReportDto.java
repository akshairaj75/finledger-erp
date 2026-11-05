package com.backend.finledger.report.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record StockValuationReportDto(
        LocalDate asOfDate,
        Long warehouseId,
        String warehouseName,
        List<StockValuationLineDto> lines,
        BigDecimal totalValuation,
        long totalItems
) {}

