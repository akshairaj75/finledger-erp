package com.backend.finledger.crm.controller;

import com.backend.finledger.crm.dto.CrmActivityDto;
import com.backend.finledger.crm.dto.CrmActivityRequest;
import com.backend.finledger.crm.entity.ActivityRelatedType;
import com.backend.finledger.crm.service.CrmActivityService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/activities")
public class CrmActivityController {

    private final CrmActivityService activityService;

    public CrmActivityController(CrmActivityService activityService) {
        this.activityService = activityService;
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('LEAD_VIEW','OPPORTUNITY_VIEW','CUSTOMER_VIEW','CRM_VIEW_ALL')")
    public ResponseEntity<List<CrmActivityDto>> getActivities(
            @RequestParam(required = false) ActivityRelatedType relatedType,
            @RequestParam(required = false) Long relatedId,
            @RequestParam(required = false) Long assignedUserId,
            @RequestParam(required = false) Boolean openOnly) {
        return ResponseEntity.ok(activityService.getActivities(relatedType, relatedId, assignedUserId, openOnly));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('LEAD_VIEW','OPPORTUNITY_VIEW','CUSTOMER_VIEW','CRM_VIEW_ALL')")
    public ResponseEntity<CrmActivityDto> getActivityById(@PathVariable Long id) {
        return ResponseEntity.ok(activityService.getActivityById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('LEAD_CREATE','LEAD_EDIT','OPPORTUNITY_CREATE','OPPORTUNITY_EDIT','CRM_VIEW_ALL')")
    public ResponseEntity<CrmActivityDto> createActivity(@Valid @RequestBody CrmActivityRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(activityService.createActivity(request));
    }

    @PatchMapping("/{id}/complete")
    @PreAuthorize("hasAnyAuthority('LEAD_EDIT','OPPORTUNITY_EDIT','CRM_VIEW_ALL')")
    public ResponseEntity<CrmActivityDto> completeActivity(@PathVariable Long id) {
        return ResponseEntity.ok(activityService.completeActivity(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('LEAD_EDIT','OPPORTUNITY_EDIT','CRM_VIEW_ALL')")
    public ResponseEntity<Void> deleteActivity(@PathVariable Long id) {
        activityService.deleteActivity(id);
        return ResponseEntity.noContent().build();
    }
}

