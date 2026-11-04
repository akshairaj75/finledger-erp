package com.backend.finledger.crm.repository;

import com.backend.finledger.crm.entity.ActivityRelatedType;
import com.backend.finledger.crm.entity.CrmActivity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CrmActivityRepository extends JpaRepository<CrmActivity, Long> {

    Optional<CrmActivity> findByCompanyIdAndId(Long companyId, Long id);

    List<CrmActivity> findByCompanyIdAndRelatedTypeAndRelatedIdOrderByDueAtDesc(
            Long companyId, ActivityRelatedType relatedType, Long relatedId);

    List<CrmActivity> findByCompanyIdAndAssignedUserIdAndCompletedAtIsNullOrderByDueAtAsc(
            Long companyId, Long assignedUserId);

    @Query("SELECT a FROM CrmActivity a WHERE a.companyId = :companyId " +
           "AND (:relatedType IS NULL OR a.relatedType = :relatedType) " +
           "AND (:relatedId IS NULL OR a.relatedId = :relatedId) " +
           "AND (:assignedUserId IS NULL OR a.assignedUserId = :assignedUserId) " +
           "AND (:openOnly IS NULL OR (:openOnly = true AND a.completedAt IS NULL) OR (:openOnly = false AND a.completedAt IS NOT NULL)) " +
           "ORDER BY a.dueAt ASC")
    List<CrmActivity> searchActivities(
            @Param("companyId") Long companyId,
            @Param("relatedType") ActivityRelatedType relatedType,
            @Param("relatedId") Long relatedId,
            @Param("assignedUserId") Long assignedUserId,
            @Param("openOnly") Boolean openOnly);
}

