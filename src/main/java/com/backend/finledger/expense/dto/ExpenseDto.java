package com.backend.finledger.expense.dto;

import com.backend.finledger.attachment.dto.AttachmentDto;
import com.backend.finledger.expense.entity.ExpenseStatus;
import com.backend.finledger.expense.entity.PaidBy;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record ExpenseDto(
    Long id,
    Long companyId,
    String expenseNo,
    LocalDate expenseDate,
    PaidBy paidBy,
    Long employeeUserId,
    String employeeName,
    Long vendorId,
    String vendorName,
    String payeeName,
    Long bankAccountId,
    String bankAccountName,
    String currencyCode,
    BigDecimal exchangeRate,
    BigDecimal subtotal,
    BigDecimal taxTotal,
    BigDecimal total,
    ExpenseStatus status,
    Instant submittedAt,
    Long approvedBy,
    String approvedByName,
    Instant approvedAt,
    String rejectionReason,
    Long journalEntryId,
    String journalEntryNo,
    Long reimbursementEntryId,
    String reimbursementEntryNo,
    List<ExpenseLineDto> lines,
    List<AttachmentDto> attachments,
    List<ExpenseApprovalDto> approvals
) {}

