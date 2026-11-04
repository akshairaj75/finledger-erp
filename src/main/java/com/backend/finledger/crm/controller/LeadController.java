package com.backend.finledger.crm.controller;

import com.backend.finledger.common.PageResponse;
import com.backend.finledger.crm.dto.*;
import com.backend.finledger.crm.entity.LeadSource;
import com.backend.finledger.crm.entity.LeadStatus;
import com.backend.finledger.crm.service.LeadConversionService;
import com.backend.finledger.crm.service.LeadService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/leads")
public class LeadController {

    private final LeadService leadService;
    private final LeadConversionService leadConversionService;

    public LeadController(LeadService leadService, LeadConversionService leadConversionService) {
        this.leadService = leadService;
        this.leadConversionService = leadConversionService;
    }

    @GetMapping
    public ResponseEntity<PageResponse<LeadDto>> getLeads(
            @RequestParam(required = false) LeadStatus status,
            @RequestParam(required = false) LeadSource source,
            @RequestParam(required = false) Long ownerUserId,
            @RequestParam(required = false) String q,
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(leadService.getLeads(status, source, ownerUserId, q, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<LeadDto> getLeadById(@PathVariable Long id) {
        return ResponseEntity.ok(leadService.getLeadById(id));
    }

    @PostMapping
    public ResponseEntity<LeadDto> createLead(@Valid @RequestBody LeadRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(leadService.createLead(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<LeadDto> updateLead(@PathVariable Long id, @Valid @RequestBody LeadRequest request) {
        return ResponseEntity.ok(leadService.updateLead(id, request));
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<LeadImportResultDto> importLeads(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(leadService.importLeadsFromCsv(file));
    }

    @PostMapping("/{id}/qualify")
    public ResponseEntity<OpportunityDto> qualifyLead(@PathVariable Long id) {
        return ResponseEntity.ok(leadService.qualifyLead(id));
    }

    @PostMapping("/{id}/convert")
    public ResponseEntity<LeadConvertResponse> convertLead(
            @PathVariable Long id,
            @RequestBody(required = false) LeadConvertRequest request) {
        LeadConvertRequest req = request != null ? request : new LeadConvertRequest(null, null, null, null, null, null);
        return ResponseEntity.ok(leadConversionService.convertLead(id, req));
    }
}
