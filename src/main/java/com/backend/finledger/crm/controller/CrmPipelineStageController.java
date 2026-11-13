package com.backend.finledger.crm.controller;

import com.backend.finledger.crm.dto.CrmPipelineStageDto;
import com.backend.finledger.crm.dto.CrmPipelineStageRequest;
import com.backend.finledger.crm.service.CrmPipelineStageService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/crm/stages")
public class CrmPipelineStageController {

    private final CrmPipelineStageService stageService;

    public CrmPipelineStageController(CrmPipelineStageService stageService) {
        this.stageService = stageService;
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('OPPORTUNITY_VIEW','CRM_VIEW_ALL','SETTINGS_VIEW','SETTINGS_EDIT','REPORT_VIEW')")
    public ResponseEntity<List<CrmPipelineStageDto>> getStages() {
        return ResponseEntity.ok(stageService.getStages());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('OPPORTUNITY_VIEW','CRM_VIEW_ALL','SETTINGS_VIEW','SETTINGS_EDIT','REPORT_VIEW')")
    public ResponseEntity<CrmPipelineStageDto> getStageById(@PathVariable Long id) {
        return ResponseEntity.ok(stageService.getStageById(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('SETTINGS_EDIT') or hasAuthority('OPPORTUNITY_EDIT')")
    public ResponseEntity<CrmPipelineStageDto> createStage(@Valid @RequestBody CrmPipelineStageRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(stageService.createStage(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('SETTINGS_EDIT') or hasAuthority('OPPORTUNITY_EDIT')")
    public ResponseEntity<CrmPipelineStageDto> updateStage(@PathVariable Long id, @Valid @RequestBody CrmPipelineStageRequest request) {
        return ResponseEntity.ok(stageService.updateStage(id, request));
    }
}

