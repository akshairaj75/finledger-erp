package com.backend.finledger.attachment.service.impl;

import com.backend.finledger.attachment.dto.AttachmentDto;
import com.backend.finledger.attachment.entity.Attachment;
import com.backend.finledger.attachment.repository.AttachmentRepository;
import com.backend.finledger.attachment.service.AttachmentService;
import com.backend.finledger.attachment.service.FileStorageService;
import com.backend.finledger.common.BusinessException;
import com.backend.finledger.expense.entity.Expense;
import com.backend.finledger.expense.entity.ExpenseStatus;
import com.backend.finledger.expense.repository.ExpenseRepository;
import com.backend.finledger.security.CurrentUser;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
public class AttachmentServiceImpl implements AttachmentService {

    private final AttachmentRepository attachmentRepository;
    private final FileStorageService fileStorageService;
    private final ExpenseRepository expenseRepository;

    public AttachmentServiceImpl(AttachmentRepository attachmentRepository,
                                 FileStorageService fileStorageService,
                                 ExpenseRepository expenseRepository) {
        this.attachmentRepository = attachmentRepository;
        this.fileStorageService = fileStorageService;
        this.expenseRepository = expenseRepository;
    }

    @Override
    @Transactional
    public AttachmentDto uploadAttachment(String entityType, Long entityId, MultipartFile file) {
        Long companyId = CurrentUser.getCompanyId();
        Long userId = CurrentUser.getUserId();

        if (entityType == null || entityType.isBlank()) {
            throw new BusinessException("INVALID_ENTITY_TYPE", "Entity type must be provided.");
        }
        if (entityId == null) {
            throw new BusinessException("INVALID_ENTITY_ID", "Entity ID must be provided.");
        }
        if (file == null || file.isEmpty()) {
            throw new BusinessException("EMPTY_FILE", "Upload file cannot be empty.");
        }

        String normalizedType = entityType.trim().toUpperCase();

        // If attaching to an expense, verify expense belongs to company and is not voided
        if ("EXPENSE".equals(normalizedType)) {
            Expense expense = expenseRepository.findByCompanyIdAndId(companyId, entityId)
                    .orElseThrow(() -> new BusinessException("EXPENSE_NOT_FOUND", "Expense not found with id: " + entityId));
            if (expense.getStatus() == ExpenseStatus.VOID) {
                throw new BusinessException("INVALID_STATUS", "Cannot attach files to a voided expense.");
            }
        }

        String storageKey = fileStorageService.store(companyId, normalizedType, entityId, file);

        Attachment attachment = new Attachment(
                companyId,
                normalizedType,
                entityId,
                file.getOriginalFilename() != null ? file.getOriginalFilename() : "attachment",
                file.getContentType() != null ? file.getContentType() : "application/octet-stream",
                file.getSize(),
                storageKey,
                userId
        );

        Attachment saved = attachmentRepository.save(attachment);
        return toDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public AttachmentDto getAttachmentById(Long id) {
        Long companyId = CurrentUser.getCompanyId();
        Attachment attachment = attachmentRepository.findByCompanyIdAndId(companyId, id)
                .orElseThrow(() -> new BusinessException("ATTACHMENT_NOT_FOUND", "Attachment not found with id: " + id));
        return toDto(attachment);
    }

    @Override
    @Transactional(readOnly = true)
    public Resource downloadAttachment(Long id) {
        Long companyId = CurrentUser.getCompanyId();
        Attachment attachment = attachmentRepository.findByCompanyIdAndId(companyId, id)
                .orElseThrow(() -> new BusinessException("ATTACHMENT_NOT_FOUND", "Attachment not found with id: " + id));
        return fileStorageService.loadAsResource(attachment.getStorageKey());
    }

    @Override
    @Transactional
    public void deleteAttachment(Long id) {
        Long companyId = CurrentUser.getCompanyId();
        Attachment attachment = attachmentRepository.findByCompanyIdAndId(companyId, id)
                .orElseThrow(() -> new BusinessException("ATTACHMENT_NOT_FOUND", "Attachment not found with id: " + id));

        // Enforce: "Remove (draft documents only)"
        if ("EXPENSE".equalsIgnoreCase(attachment.getEntityType())) {
            Expense expense = expenseRepository.findByCompanyIdAndId(companyId, attachment.getEntityId()).orElse(null);
            if (expense != null && expense.getStatus() != ExpenseStatus.DRAFT) {
                throw new BusinessException("ATTACHMENT_LOCKED", "Attachments can only be deleted for draft documents.");
            }
        }

        fileStorageService.delete(attachment.getStorageKey());
        attachmentRepository.delete(attachment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AttachmentDto> getAttachmentsByEntity(String entityType, Long entityId) {
        Long companyId = CurrentUser.getCompanyId();
        return attachmentRepository.findByCompanyIdAndEntityTypeAndEntityId(companyId, entityType.toUpperCase(), entityId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public long countAttachments(String entityType, Long entityId) {
        Long companyId = CurrentUser.getCompanyId();
        return attachmentRepository.countByCompanyIdAndEntityTypeAndEntityId(companyId, entityType.toUpperCase(), entityId);
    }

    private AttachmentDto toDto(Attachment a) {
        return new AttachmentDto(
                a.getId(),
                a.getCompanyId(),
                a.getEntityType(),
                a.getEntityId(),
                a.getFileName(),
                a.getContentType(),
                a.getSizeBytes(),
                a.getStorageKey(),
                a.getUploadedBy(),
                a.getUploadedAt()
        );
    }
}

