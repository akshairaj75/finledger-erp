package com.backend.finledger.crm.repository;

import com.backend.finledger.crm.entity.Opportunity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OpportunityRepository extends JpaRepository<Opportunity, Long> {

    Optional<Opportunity> findByCompanyIdAndId(Long companyId, Long id);

    Optional<Opportunity> findByCompanyIdAndOpportunityNo(Long companyId, String opportunityNo);

    List<Opportunity> findByCompanyId(Long companyId);

    List<Opportunity> findByCompanyIdAndStageId(Long companyId, Long stageId);

    @Query("SELECT o FROM Opportunity o WHERE o.companyId = :companyId " +
           "AND (:stageId IS NULL OR o.stageId = :stageId) " +
           "AND (:customerId IS NULL OR o.customerId = :customerId) " +
           "AND (:ownerUserId IS NULL OR o.ownerUserId = :ownerUserId) " +
           "AND (:q IS NULL OR LOWER(o.title) LIKE LOWER(CONCAT('%', :q, '%')) " +
           "     OR LOWER(o.opportunityNo) LIKE LOWER(CONCAT('%', :q, '%')))")
    Page<Opportunity> searchOpportunities(
            @Param("companyId") Long companyId,
            @Param("stageId") Long stageId,
            @Param("customerId") Long customerId,
            @Param("ownerUserId") Long ownerUserId,
            @Param("q") String q,
            Pageable pageable);
}

