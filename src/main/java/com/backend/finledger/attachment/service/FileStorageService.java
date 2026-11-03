package com.backend.finledger.attachment.service;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {

    /**
     * Stores an uploaded multipart file externally (local volume or S3-compatible storage)
     * and returns the unique storage key.
     */
    String store(Long companyId, String entityType, Long entityId, MultipartFile file);

    /**
     * Loads the stored file as a Spring Resource.
     */
    Resource loadAsResource(String storageKey);

    /**
     * Deletes the physical file associated with the storage key.
     */
    void delete(String storageKey);
}

