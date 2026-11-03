package com.backend.finledger.attachment.service;

import com.backend.finledger.attachment.dto.AttachmentDto;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface AttachmentService {

    AttachmentDto uploadAttachment(String entityType, Long entityId, MultipartFile file);

    AttachmentDto getAttachmentById(Long id);

    Resource downloadAttachment(Long id);

    void deleteAttachment(Long id);

    List<AttachmentDto> getAttachmentsByEntity(String entityType, Long entityId);

    long countAttachments(String entityType, Long entityId);
}

