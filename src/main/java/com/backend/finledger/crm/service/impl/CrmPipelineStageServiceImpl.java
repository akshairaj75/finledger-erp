package com.backend.finledger.crm.service.impl;

import com.backend.finledger.common.BusinessException;
import com.backend.finledger.crm.dto.CrmPipelineStageDto;
import com.backend.finledger.crm.dto.CrmPipelineStageRequest;
import com.backend.finledger.crm.entity.CrmPipelineStage;
import com.backend.finledger.crm.entity.StageType;
import com.backend.finledger.crm.repository.CrmPipelineStageRepository;
import com.backend.finledger.crm.service.CrmPipelineStageService;
import com.backend.finledger.security.CurrentUser;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CrmPipelineStageServiceImpl implements CrmPipelineStageService {

    private final CrmPipelineStageRepository stageRepository;

    public CrmPipelineStageServiceImpl(CrmPipelineStageRepository stageRepository) {
        this.stageRepository = stageRepository;
    }

    @Override
    @Transactional
    @PreAuthorize("hasAnyAuthority('OPPORTUNITY_VIEW','CRM_VIEW_ALL','SETTINGS_VIEW','SETTINGS_EDIT','REPORT_VIEW')")
    public List<CrmPipelineStageDto> getStages() {
        Long companyId = CurrentUser.getCompanyId();
        initDefaultStagesIfEmpty(companyId);
        return stageRepository.findByCompanyIdOrderBySortOrderAsc(companyId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyAuthority('OPPORTUNITY_VIEW','CRM_VIEW_ALL','SETTINGS_VIEW','SETTINGS_EDIT','REPORT_VIEW')")
    public CrmPipelineStageDto getStageById(Long id) {
        Long companyId = CurrentUser.getCompanyId();
        CrmPipelineStage stage = stageRepository.findByCompanyIdAndId(companyId, id)
                .orElseThrow(() -> new BusinessException("STAGE_NOT_FOUND", "Pipeline stage not found with id: " + id));
        return toDto(stage);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('SETTINGS_EDIT') or hasAuthority('OPPORTUNITY_EDIT')")
    public CrmPipelineStageDto createStage(CrmPipelineStageRequest request) {
        Long companyId = CurrentUser.getCompanyId();

        if (stageRepository.existsByCompanyIdAndName(companyId, request.name().trim())) {
            throw new BusinessException("DUPLICATE_STAGE_NAME", "Pipeline stage name already exists: " + request.name());
        }

        CrmPipelineStage stage = new CrmPipelineStage();
        stage.setCompanyId(companyId);
        stage.setName(request.name().trim());
        stage.setSortOrder(request.sortOrder());
        stage.setProbabilityPct(request.probabilityPct() != null ? request.probabilityPct() : (byte) 0);
        stage.setStageType(request.stageType() != null ? request.stageType() : StageType.OPEN);

        CrmPipelineStage saved = stageRepository.save(stage);
        return toDto(saved);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('SETTINGS_EDIT') or hasAuthority('OPPORTUNITY_EDIT')")
    public CrmPipelineStageDto updateStage(Long id, CrmPipelineStageRequest request) {
        Long companyId = CurrentUser.getCompanyId();
        CrmPipelineStage stage = stageRepository.findByCompanyIdAndId(companyId, id)
                .orElseThrow(() -> new BusinessException("STAGE_NOT_FOUND", "Pipeline stage not found with id: " + id));

        if (!stage.getName().equalsIgnoreCase(request.name().trim()) &&
                stageRepository.existsByCompanyIdAndName(companyId, request.name().trim())) {
            throw new BusinessException("DUPLICATE_STAGE_NAME", "Pipeline stage name already exists: " + request.name());
        }

        stage.setName(request.name().trim());
        stage.setSortOrder(request.sortOrder());
        if (request.probabilityPct() != null) {
            stage.setProbabilityPct(request.probabilityPct());
        }
        if (request.stageType() != null) {
            stage.setStageType(request.stageType());
        }

        CrmPipelineStage saved = stageRepository.save(stage);
        return toDto(saved);
    }

    @Override
    @Transactional
    public void initDefaultStagesIfEmpty(Long companyId) {
        if (stageRepository.findByCompanyIdOrderBySortOrderAsc(companyId).isEmpty()) {
            List<CrmPipelineStage> defaults = List.of(
                    new CrmPipelineStage(companyId, "New", (byte) 1, (byte) 10, StageType.OPEN),
                    new CrmPipelineStage(companyId, "Qualified", (byte) 2, (byte) 25, StageType.OPEN),
                    new CrmPipelineStage(companyId, "Proposal", (byte) 3, (byte) 50, StageType.OPEN),
                    new CrmPipelineStage(companyId, "Negotiation", (byte) 4, (byte) 75, StageType.OPEN),
                    new CrmPipelineStage(companyId, "Won", (byte) 5, (byte) 100, StageType.WON),
                    new CrmPipelineStage(companyId, "Lost", (byte) 6, (byte) 0, StageType.LOST)
            );
            stageRepository.saveAll(defaults);
        }
    }

    private CrmPipelineStageDto toDto(CrmPipelineStage stage) {
        return new CrmPipelineStageDto(
                stage.getId(),
                stage.getCompanyId(),
                stage.getName(),
                stage.getSortOrder(),
                stage.getProbabilityPct(),
                stage.getStageType()
        );
    }
}

