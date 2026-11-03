package com.backend.finledger.attachment.dto;

import java.time.Instant;

public record AttachmentDto(
    Long id,
    Long companyId,
    String entityType,
    Long entityId,
    String fileName,
    String contentType,
    Long sizeBytes,
    String storageKey,
    Long uploadedBy,
    Instant uploadedAt
) {}

