package com.backend.finledger.crm.service.impl;

import com.backend.finledger.common.BusinessException;
import com.backend.finledger.crm.dto.CrmActivityDto;
import com.backend.finledger.crm.dto.CrmActivityRequest;
import com.backend.finledger.crm.entity.ActivityRelatedType;
import com.backend.finledger.crm.entity.CrmActivity;
import com.backend.finledger.crm.repository.CrmActivityRepository;
import com.backend.finledger.crm.service.CrmActivityService;
import com.backend.finledger.security.CurrentUser;
import com.backend.finledger.user.entity.User;
import com.backend.finledger.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@Transactional
public class CrmActivityServiceImpl implements CrmActivityService {

    private final CrmActivityRepository activityRepository;
    private final UserRepository userRepository;

    public CrmActivityServiceImpl(CrmActivityRepository activityRepository,
                                  UserRepository userRepository) {
        this.activityRepository = activityRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CrmActivityDto> getActivities(ActivityRelatedType relatedType, Long relatedId, Long assignedUserId, Boolean openOnly) {
        Long companyId = CurrentUser.getCompanyId();
        List<CrmActivity> activities = activityRepository.searchActivities(companyId, relatedType, relatedId, assignedUserId, openOnly);
        return activities.stream().map(this::mapToDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CrmActivityDto getActivityById(Long id) {
        Long companyId = CurrentUser.getCompanyId();
        CrmActivity activity = activityRepository.findByCompanyIdAndId(companyId, id)
                .orElseThrow(() -> new BusinessException("ACTIVITY_NOT_FOUND", "Activity not found with id: " + id));
        return mapToDto(activity);
    }

    @Override
    public CrmActivityDto createActivity(CrmActivityRequest request) {
        Long companyId = CurrentUser.getCompanyId();

        CrmActivity activity = new CrmActivity();
        activity.setCompanyId(companyId);
        activity.setActivityType(request.activityType());
        activity.setSubject(request.subject());
        activity.setDetails(request.details());
        activity.setRelatedType(request.relatedType());
        activity.setRelatedId(request.relatedId());
        activity.setDueAt(request.dueAt());
        activity.setAssignedUserId(request.assignedUserId());

        CrmActivity saved = activityRepository.save(activity);
        return mapToDto(saved);
    }

    @Override
    public CrmActivityDto completeActivity(Long id) {
        Long companyId = CurrentUser.getCompanyId();
        CrmActivity activity = activityRepository.findByCompanyIdAndId(companyId, id)
                .orElseThrow(() -> new BusinessException("ACTIVITY_NOT_FOUND", "Activity not found with id: " + id));

        if (activity.getCompletedAt() == null) {
            activity.setCompletedAt(Instant.now());
            activity = activityRepository.save(activity);
        }

        return mapToDto(activity);
    }

    @Override
    public void deleteActivity(Long id) {
        Long companyId = CurrentUser.getCompanyId();
        CrmActivity activity = activityRepository.findByCompanyIdAndId(companyId, id)
                .orElseThrow(() -> new BusinessException("ACTIVITY_NOT_FOUND", "Activity not found with id: " + id));
        activityRepository.delete(activity);
    }

    private CrmActivityDto mapToDto(CrmActivity activity) {
        String assignedUserName = null;
        if (activity.getAssignedUserId() != null) {
            assignedUserName = userRepository.findById(activity.getAssignedUserId())
                    .map(User::getFullName)
                    .orElse(null);
        }

        return new CrmActivityDto(
                activity.getId(),
                activity.getCompanyId(),
                activity.getActivityType(),
                activity.getSubject(),
                activity.getDetails(),
                activity.getRelatedType(),
                activity.getRelatedId(),
                activity.getDueAt(),
                activity.getCompletedAt(),
                activity.getAssignedUserId(),
                assignedUserName,
                activity.getCreatedAt()
        );
    }
}

