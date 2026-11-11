package com.backend.finledger.audit.repository;

import com.backend.finledger.audit.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    List<AuditLog> findByEntityNameAndEntityIdOrderByCreatedAtDesc(String entityName, Long entityId);

    List<AuditLog> findByCompanyIdOrderByCreatedAtDesc(Long companyId);

    @Query("SELECT a FROM AuditLog a WHERE a.companyId = :companyId " +
           "AND (:entityName IS NULL OR LOWER(a.entityName) LIKE LOWER(CONCAT('%', :entityName, '%'))) " +
           "AND (:userId IS NULL OR a.userId = :userId) " +
           "AND (:fromInstant IS NULL OR a.createdAt >= :fromInstant) " +
           "AND (:toInstant IS NULL OR a.createdAt <= :toInstant) " +
           "ORDER BY a.createdAt DESC")
    Page<AuditLog> searchLogs(
            @Param("companyId") Long companyId,
            @Param("entityName") String entityName,
            @Param("userId") Long userId,
            @Param("fromInstant") Instant fromInstant,
            @Param("toInstant") Instant toInstant,
            Pageable pageable
    );
}
