package com.backend.finledger.crm.repository;

import com.backend.finledger.crm.entity.Lead;
import com.backend.finledger.crm.entity.LeadSource;
import com.backend.finledger.crm.entity.LeadStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LeadRepository extends JpaRepository<Lead, Long> {

    Optional<Lead> findByCompanyIdAndId(Long companyId, Long id);

    Optional<Lead> findByCompanyIdAndLeadNo(Long companyId, String leadNo);

    Optional<Lead> findFirstByCompanyIdAndEmailIgnoreCase(Long companyId, String email);

    @Query("SELECT l FROM Lead l WHERE l.companyId = :companyId " +
           "AND (:status IS NULL OR l.status = :status) " +
           "AND (:source IS NULL OR l.source = :source) " +
           "AND (:ownerUserId IS NULL OR l.ownerUserId = :ownerUserId) " +
           "AND (:q IS NULL OR LOWER(l.companyName) LIKE LOWER(CONCAT('%', :q, '%')) " +
           "     OR LOWER(l.contactName) LIKE LOWER(CONCAT('%', :q, '%')) " +
           "     OR LOWER(l.email) LIKE LOWER(CONCAT('%', :q, '%')) " +
           "     OR LOWER(l.leadNo) LIKE LOWER(CONCAT('%', :q, '%')))")
    Page<Lead> searchLeads(
            @Param("companyId") Long companyId,
            @Param("status") LeadStatus status,
            @Param("source") LeadSource source,
            @Param("ownerUserId") Long ownerUserId,
            @Param("q") String q,
            Pageable pageable);
}

