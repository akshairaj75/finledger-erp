package com.backend.finledger.crm.controller;

import com.backend.finledger.crm.dto.CrmPipelineStageDto;
import com.backend.finledger.crm.dto.CrmPipelineStageRequest;
import com.backend.finledger.crm.service.CrmPipelineStageService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<List<CrmPipelineStageDto>> getStages() {
        return ResponseEntity.ok(stageService.getStages());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CrmPipelineStageDto> getStageById(@PathVariable Long id) {
        return ResponseEntity.ok(stageService.getStageById(id));
    }

    @PostMapping
    public ResponseEntity<CrmPipelineStageDto> createStage(@Valid @RequestBody CrmPipelineStageRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(stageService.createStage(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CrmPipelineStageDto> updateStage(@PathVariable Long id, @Valid @RequestBody CrmPipelineStageRequest request) {
        return ResponseEntity.ok(stageService.updateStage(id, request));
    }
}

