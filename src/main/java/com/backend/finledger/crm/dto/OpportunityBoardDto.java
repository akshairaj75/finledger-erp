package com.backend.finledger.crm.dto;

import java.math.BigDecimal;
import java.util.List;

public record OpportunityBoardDto(
    List<OpportunityStageColumnDto> columns,
    BigDecimal grandTotalValue,
    int totalOpportunities
) {}

