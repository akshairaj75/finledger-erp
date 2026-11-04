package com.backend.finledger.crm.service;

import com.backend.finledger.crm.dto.CrmActivityDto;
import com.backend.finledger.crm.dto.CrmActivityRequest;
import com.backend.finledger.crm.entity.ActivityRelatedType;

import java.util.List;

public interface CrmActivityService {
    List<CrmActivityDto> getActivities(ActivityRelatedType relatedType, Long relatedId, Long assignedUserId, Boolean openOnly);
    CrmActivityDto getActivityById(Long id);
    CrmActivityDto createActivity(CrmActivityRequest request);
    CrmActivityDto completeActivity(Long id);
    void deleteActivity(Long id);
}

