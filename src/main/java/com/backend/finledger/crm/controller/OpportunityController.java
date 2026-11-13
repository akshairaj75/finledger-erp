package com.backend.finledger.crm.controller;

import com.backend.finledger.common.PageResponse;
import com.backend.finledger.crm.dto.OpportunityBoardDto;
import com.backend.finledger.crm.dto.OpportunityDto;
import com.backend.finledger.crm.dto.OpportunityRequest;
import com.backend.finledger.crm.dto.OpportunityStageRequest;
import com.backend.finledger.crm.service.OpportunityService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/opportunities")
public class OpportunityController {

    private final OpportunityService opportunityService;

    public OpportunityController(OpportunityService opportunityService) {
        this.opportunityService = opportunityService;
    }

    @GetMapping("/board")
    @PreAuthorize("hasAuthority('OPPORTUNITY_VIEW') or hasAuthority('CRM_VIEW_ALL')")
    public ResponseEntity<OpportunityBoardDto> getOpportunityBoard() {
        return ResponseEntity.ok(opportunityService.getOpportunityBoard());
    }

    @GetMapping
    @PreAuthorize("hasAuthority('OPPORTUNITY_VIEW') or hasAuthority('CRM_VIEW_ALL')")
    public ResponseEntity<PageResponse<OpportunityDto>> getOpportunities(
            @RequestParam(required = false) Long stageId,
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) Long ownerUserId,
            @RequestParam(required = false) String q,
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(opportunityService.getOpportunities(stageId, customerId, ownerUserId, q, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('OPPORTUNITY_VIEW') or hasAuthority('CRM_VIEW_ALL')")
    public ResponseEntity<OpportunityDto> getOpportunityById(@PathVariable Long id) {
        return ResponseEntity.ok(opportunityService.getOpportunityById(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('OPPORTUNITY_CREATE')")
    public ResponseEntity<OpportunityDto> createOpportunity(@Valid @RequestBody OpportunityRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(opportunityService.createOpportunity(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('OPPORTUNITY_EDIT')")
    public ResponseEntity<OpportunityDto> updateOpportunity(@PathVariable Long id, @Valid @RequestBody OpportunityRequest request) {
        return ResponseEntity.ok(opportunityService.updateOpportunity(id, request));
    }

    @PatchMapping("/{id}/stage")
    @PreAuthorize("hasAuthority('OPPORTUNITY_EDIT')")
    public ResponseEntity<OpportunityDto> updateOpportunityStage(@PathVariable Long id, @Valid @RequestBody OpportunityStageRequest request) {
        return ResponseEntity.ok(opportunityService.updateOpportunityStage(id, request));
    }
}

