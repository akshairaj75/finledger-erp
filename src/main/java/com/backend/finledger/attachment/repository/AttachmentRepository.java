package com.backend.finledger.attachment.repository;

import com.backend.finledger.attachment.entity.Attachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AttachmentRepository extends JpaRepository<Attachment, Long> {

    List<Attachment> findByCompanyIdAndEntityTypeAndEntityId(Long companyId, String entityType, Long entityId);

    Optional<Attachment> findByCompanyIdAndId(Long companyId, Long id);

    long countByCompanyIdAndEntityTypeAndEntityId(Long companyId, String entityType, Long entityId);

    void deleteByCompanyIdAndEntityTypeAndEntityId(Long companyId, String entityType, Long entityId);
}

