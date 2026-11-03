package com.backend.finledger.audit.repository;

import com.backend.finledger.audit.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    List<AuditLog> findByEntityNameAndEntityIdOrderByCreatedAtDesc(String entityName, Long entityId);

    List<AuditLog> findByCompanyIdOrderByCreatedAtDesc(Long companyId);
}

