package com.backend.finledger.attachment.entity;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * Attachment entity matching the attachments table.
 * Files (receipt images, PDFs) attached to any document — used heavily by expenses.
 */
@Entity
@Table(name = "attachments", indexes = {
    @Index(name = "ix_att_entity", columnList = "entity_type, entity_id")
})
public class Attachment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "company_id", nullable = false)
    private Long companyId;

    @Column(name = "entity_type", length = 40, nullable = false)
    private String entityType;

    @Column(name = "entity_id", nullable = false)
    private Long entityId;

    @Column(name = "file_name", length = 255, nullable = false)
    private String fileName;

    @Column(name = "content_type", length = 100, nullable = false)
    private String contentType;

    @Column(name = "size_bytes", nullable = false)
    private Integer sizeBytes;

    @Column(name = "storage_key", length = 500, nullable = false)
    private String storageKey;

    @Column(name = "uploaded_by")
    private Long uploadedBy;

    @Column(name = "uploaded_at", nullable = false)
    private Instant uploadedAt = Instant.now();

    public Attachment() {}

    public Attachment(Long companyId, String entityType, Long entityId, String fileName,
                      String contentType, Long sizeBytes, String storageKey, Long uploadedBy) {
        this.companyId = companyId;
        this.entityType = entityType;
        this.entityId = entityId;
        this.fileName = fileName;
        this.contentType = contentType;
        this.sizeBytes = sizeBytes != null ? sizeBytes.intValue() : 0;
        this.storageKey = storageKey;
        this.uploadedBy = uploadedBy;
        this.uploadedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getCompanyId() {
        return companyId;
    }

    public void setCompanyId(Long companyId) {
        this.companyId = companyId;
    }

    public String getEntityType() {
        return entityType;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public Long getEntityId() {
        return entityId;
    }

    public void setEntityId(Long entityId) {
        this.entityId = entityId;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public Long getSizeBytes() {
        return sizeBytes != null ? sizeBytes.longValue() : 0L;
    }

    public void setSizeBytes(Long sizeBytes) {
        this.sizeBytes = sizeBytes != null ? sizeBytes.intValue() : 0;
    }

    public void setSizeBytes(Integer sizeBytes) {
        this.sizeBytes = sizeBytes;
    }

    public String getStorageKey() {
        return storageKey;
    }

    public void setStorageKey(String storageKey) {
        this.storageKey = storageKey;
    }

    public Long getUploadedBy() {
        return uploadedBy;
    }

    public void setUploadedBy(Long uploadedBy) {
        this.uploadedBy = uploadedBy;
    }

    public Instant getUploadedAt() {
        return uploadedAt;
    }

    public void setUploadedAt(Instant uploadedAt) {
        this.uploadedAt = uploadedAt;
    }
}
