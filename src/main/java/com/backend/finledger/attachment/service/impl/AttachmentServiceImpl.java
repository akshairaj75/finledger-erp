package com.backend.finledger.attachment.service.impl;

import com.backend.finledger.attachment.dto.AttachmentDto;
import com.backend.finledger.attachment.entity.Attachment;
import com.backend.finledger.attachment.repository.AttachmentRepository;
import com.backend.finledger.attachment.service.AttachmentService;
import com.backend.finledger.attachment.service.FileStorageService;
import com.backend.finledger.common.BusinessException;
import com.backend.finledger.common.ForbiddenException;
import com.backend.finledger.crm.entity.Quotation;
import com.backend.finledger.crm.entity.QuotationStatus;
import com.backend.finledger.crm.repository.QuotationRepository;
import com.backend.finledger.expense.entity.Expense;
import com.backend.finledger.expense.entity.ExpenseStatus;
import com.backend.finledger.expense.repository.ExpenseRepository;
import com.backend.finledger.inventory.entity.AdjustmentStatus;
import com.backend.finledger.inventory.entity.StockAdjustment;
import com.backend.finledger.inventory.repository.StockAdjustmentRepository;
import com.backend.finledger.ledger.entity.JournalEntry;
import com.backend.finledger.ledger.entity.JournalStatus;
import com.backend.finledger.ledger.repository.JournalEntryRepository;
import com.backend.finledger.purchases.entity.Bill;
import com.backend.finledger.purchases.entity.BillStatus;
import com.backend.finledger.purchases.repository.BillRepository;
import com.backend.finledger.sales.entity.Invoice;
import com.backend.finledger.sales.entity.InvoiceStatus;
import com.backend.finledger.sales.repository.InvoiceRepository;
import com.backend.finledger.security.CurrentUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Objects;

@Service
public class AttachmentServiceImpl implements AttachmentService {

    private final AttachmentRepository attachmentRepository;
    private final FileStorageService fileStorageService;
    private final ExpenseRepository expenseRepository;
    private final InvoiceRepository invoiceRepository;
    private final BillRepository billRepository;
    private final QuotationRepository quotationRepository;
    private final JournalEntryRepository journalEntryRepository;
    private final StockAdjustmentRepository stockAdjustmentRepository;

    public AttachmentServiceImpl(AttachmentRepository attachmentRepository,
                                 FileStorageService fileStorageService,
                                 ExpenseRepository expenseRepository) {
        this(attachmentRepository, fileStorageService, expenseRepository, null, null, null, null, null);
    }

    @Autowired
    public AttachmentServiceImpl(AttachmentRepository attachmentRepository,
                                 FileStorageService fileStorageService,
                                 ExpenseRepository expenseRepository,
                                 @Autowired(required = false) InvoiceRepository invoiceRepository,
                                 @Autowired(required = false) BillRepository billRepository,
                                 @Autowired(required = false) QuotationRepository quotationRepository,
                                 @Autowired(required = false) JournalEntryRepository journalEntryRepository,
                                 @Autowired(required = false) StockAdjustmentRepository stockAdjustmentRepository) {
        this.attachmentRepository = attachmentRepository;
        this.fileStorageService = fileStorageService;
        this.expenseRepository = expenseRepository;
        this.invoiceRepository = invoiceRepository;
        this.billRepository = billRepository;
        this.quotationRepository = quotationRepository;
        this.journalEntryRepository = journalEntryRepository;
        this.stockAdjustmentRepository = stockAdjustmentRepository;
    }

    @Override
    @Transactional
    @PreAuthorize("hasAnyAuthority('EXPENSE_CREATE','EXPENSE_APPROVE','INVOICE_CREATE','BILL_CREATE','JOURNAL_CREATE','CUSTOMER_EDIT','VENDOR_EDIT','LEAD_EDIT','OPPORTUNITY_EDIT','SETTINGS_EDIT')")
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

        // If attaching to an expense, verify expense belongs to company, access scope, and is not voided
        if ("EXPENSE".equals(normalizedType)) {
            Expense expense = expenseRepository.findByCompanyIdAndId(companyId, entityId)
                    .orElseThrow(() -> new BusinessException("EXPENSE_NOT_FOUND", "Expense not found with id: " + entityId));
            assertExpenseAttachmentAccess(expense);
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
    @PreAuthorize("hasAnyAuthority('EXPENSE_VIEW','EXPENSE_CREATE','EXPENSE_APPROVE','EXPENSE_REIMBURSE','INVOICE_VIEW','INVOICE_CREATE','BILL_VIEW','BILL_CREATE','JOURNAL_VIEW','CUSTOMER_VIEW','VENDOR_VIEW','LEAD_VIEW','OPPORTUNITY_VIEW','SETTINGS_EDIT')")
    public AttachmentDto getAttachmentById(Long id) {
        Long companyId = CurrentUser.getCompanyId();
        Attachment attachment = attachmentRepository.findByCompanyIdAndId(companyId, id)
                .orElseThrow(() -> new BusinessException("ATTACHMENT_NOT_FOUND", "Attachment not found with id: " + id));
        assertExpenseAttachmentAccessIfApplicable(companyId, attachment.getEntityType(), attachment.getEntityId());
        return toDto(attachment);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyAuthority('EXPENSE_VIEW','EXPENSE_CREATE','EXPENSE_APPROVE','EXPENSE_REIMBURSE','INVOICE_VIEW','INVOICE_CREATE','BILL_VIEW','BILL_CREATE','JOURNAL_VIEW','CUSTOMER_VIEW','VENDOR_VIEW','LEAD_VIEW','OPPORTUNITY_VIEW','SETTINGS_EDIT')")
    public Resource downloadAttachment(Long id) {
        Long companyId = CurrentUser.getCompanyId();
        Attachment attachment = attachmentRepository.findByCompanyIdAndId(companyId, id)
                .orElseThrow(() -> new BusinessException("ATTACHMENT_NOT_FOUND", "Attachment not found with id: " + id));
        assertExpenseAttachmentAccessIfApplicable(companyId, attachment.getEntityType(), attachment.getEntityId());
        return fileStorageService.loadAsResource(attachment.getStorageKey());
    }

    @Override
    @Transactional
    @PreAuthorize("hasAnyAuthority('EXPENSE_CREATE','EXPENSE_APPROVE','INVOICE_CREATE','BILL_CREATE','JOURNAL_CREATE','CUSTOMER_EDIT','VENDOR_EDIT','LEAD_EDIT','OPPORTUNITY_EDIT','SETTINGS_EDIT')")
    public void deleteAttachment(Long id) {
        Long companyId = CurrentUser.getCompanyId();
        Attachment attachment = attachmentRepository.findByCompanyIdAndId(companyId, id)
                .orElseThrow(() -> new BusinessException("ATTACHMENT_NOT_FOUND", "Attachment not found with id: " + id));

        String entityType = attachment.getEntityType() != null ? attachment.getEntityType().trim().toUpperCase() : "";
        Long entityId = attachment.getEntityId();

        // Enforce: "Remove (draft documents only)" across all stateful entity types
        if ("EXPENSE".equals(entityType)) {
            Expense expense = expenseRepository.findByCompanyIdAndId(companyId, entityId).orElse(null);
            if (expense != null) {
                assertExpenseAttachmentAccess(expense);
                if (expense.getStatus() != ExpenseStatus.DRAFT) {
                    throw new BusinessException("ATTACHMENT_LOCKED", "Attachments can only be deleted for draft documents.");
                }
            }
        } else if ("INVOICE".equals(entityType) && invoiceRepository != null) {
            Invoice invoice = invoiceRepository.findByCompanyIdAndId(companyId, entityId).orElse(null);
            if (invoice != null && invoice.getStatus() != InvoiceStatus.DRAFT) {
                throw new BusinessException("ATTACHMENT_LOCKED", "Attachments can only be deleted for draft documents.");
            }
        } else if ("BILL".equals(entityType) && billRepository != null) {
            Bill bill = billRepository.findByCompanyIdAndId(companyId, entityId).orElse(null);
            if (bill != null && bill.getStatus() != BillStatus.DRAFT) {
                throw new BusinessException("ATTACHMENT_LOCKED", "Attachments can only be deleted for draft documents.");
            }
        } else if ("QUOTATION".equals(entityType) && quotationRepository != null) {
            Quotation quotation = quotationRepository.findByCompanyIdAndId(companyId, entityId).orElse(null);
            if (quotation != null && quotation.getStatus() != QuotationStatus.DRAFT) {
                throw new BusinessException("ATTACHMENT_LOCKED", "Attachments can only be deleted for draft documents.");
            }
        } else if (("JOURNAL".equals(entityType) || "JOURNAL_ENTRY".equals(entityType)) && journalEntryRepository != null) {
            JournalEntry journalEntry = journalEntryRepository.findByCompanyIdAndId(companyId, entityId).orElse(null);
            if (journalEntry != null && journalEntry.getStatus() != JournalStatus.DRAFT) {
                throw new BusinessException("ATTACHMENT_LOCKED", "Attachments can only be deleted for draft documents.");
            }
        } else if (("STOCK_ADJUSTMENT".equals(entityType) || "ADJUSTMENT".equals(entityType)) && stockAdjustmentRepository != null) {
            StockAdjustment adjustment = stockAdjustmentRepository.findByCompanyIdAndId(companyId, entityId).orElse(null);
            if (adjustment != null && adjustment.getStatus() != AdjustmentStatus.DRAFT) {
                throw new BusinessException("ATTACHMENT_LOCKED", "Attachments can only be deleted for draft documents.");
            }
        }

        fileStorageService.delete(attachment.getStorageKey());
        attachmentRepository.delete(attachment);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyAuthority('EXPENSE_VIEW','EXPENSE_CREATE','EXPENSE_APPROVE','EXPENSE_REIMBURSE','INVOICE_VIEW','INVOICE_CREATE','BILL_VIEW','BILL_CREATE','JOURNAL_VIEW','CUSTOMER_VIEW','VENDOR_VIEW','LEAD_VIEW','OPPORTUNITY_VIEW','SETTINGS_EDIT')")
    public List<AttachmentDto> getAttachmentsByEntity(String entityType, Long entityId) {
        Long companyId = CurrentUser.getCompanyId();
        String normalizedType = entityType.toUpperCase();
        assertExpenseAttachmentAccessIfApplicable(companyId, normalizedType, entityId);
        return attachmentRepository.findByCompanyIdAndEntityTypeAndEntityId(companyId, normalizedType, entityId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    private void assertExpenseAttachmentAccessIfApplicable(Long companyId, String entityType, Long entityId) {
        if ("EXPENSE".equalsIgnoreCase(entityType)) {
            expenseRepository.findByCompanyIdAndId(companyId, entityId)
                    .ifPresent(this::assertExpenseAttachmentAccess);
        }
    }

    private void assertExpenseAttachmentAccess(Expense expense) {
        if (CurrentUser.hasAnyAuthority("EXPENSE_APPROVE", "EXPENSE_REIMBURSE", "REPORT_VIEW", "SETTINGS_EDIT")) {
            return;
        }
        Long currentUserId = CurrentUser.getUserId();
        if (!Objects.equals(expense.getEmployeeUserId(), currentUserId)
                && !Objects.equals(expense.getCreatedBy(), currentUserId)) {
            throw new ForbiddenException("ACCESS_DENIED", "You do not have permission to access attachments for this expense.");
        }
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

