package com.backend.finledger.crm.repository;

import com.backend.finledger.crm.entity.CrmPipelineStage;
import com.backend.finledger.crm.entity.StageType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CrmPipelineStageRepository extends JpaRepository<CrmPipelineStage, Long> {

    List<CrmPipelineStage> findByCompanyIdOrderBySortOrderAsc(Long companyId);

    Optional<CrmPipelineStage> findByCompanyIdAndId(Long companyId, Long id);

    Optional<CrmPipelineStage> findByCompanyIdAndName(Long companyId, String name);

    Optional<CrmPipelineStage> findFirstByCompanyIdAndStageTypeOrderBySortOrderAsc(Long companyId, StageType stageType);

    boolean existsByCompanyIdAndName(Long companyId, String name);
}

