package com.backend.finledger.crm.service;

import com.backend.finledger.common.PageResponse;
import com.backend.finledger.crm.dto.OpportunityBoardDto;
import com.backend.finledger.crm.dto.OpportunityDto;
import com.backend.finledger.crm.dto.OpportunityRequest;
import com.backend.finledger.crm.dto.OpportunityStageRequest;
import org.springframework.data.domain.Pageable;

public interface OpportunityService {

    PageResponse<OpportunityDto> getOpportunities(Long stageId, Long customerId, Long ownerUserId, String q, Pageable pageable);

    OpportunityDto getOpportunityById(Long id);

    OpportunityBoardDto getOpportunityBoard();

    OpportunityDto createOpportunity(OpportunityRequest request);

    OpportunityDto updateOpportunity(Long id, OpportunityRequest request);

    OpportunityDto updateOpportunityStage(Long id, OpportunityStageRequest request);
}

