package com.backend.finledger.crm.service;

import com.backend.finledger.crm.dto.CrmPipelineStageDto;
import com.backend.finledger.crm.dto.CrmPipelineStageRequest;

import java.util.List;

public interface CrmPipelineStageService {

    List<CrmPipelineStageDto> getStages();

    CrmPipelineStageDto getStageById(Long id);

    CrmPipelineStageDto createStage(CrmPipelineStageRequest request);

    CrmPipelineStageDto updateStage(Long id, CrmPipelineStageRequest request);

    void initDefaultStagesIfEmpty(Long companyId);
}

