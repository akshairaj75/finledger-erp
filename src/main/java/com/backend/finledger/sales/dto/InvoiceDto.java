package com.backend.finledger.sales.dto;

import com.backend.finledger.sales.entity.InvoiceDocType;
import com.backend.finledger.sales.entity.InvoiceStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record InvoiceDto(
        Long id,
        Long companyId,
        String invoiceNo,
        InvoiceDocType docType,
        Long customerId,
        String customerName,
        String customerCode,
        LocalDate invoiceDate,
        LocalDate dueDate,
        String currencyCode,
        BigDecimal exchangeRate,
        BigDecimal subtotal,
        BigDecimal discountTotal,
        BigDecimal taxTotal,
        BigDecimal total,
        BigDecimal amountPaid,
        BigDecimal balanceDue,
        InvoiceStatus status,
        String notes,
        Long quotationId,
        Long salespersonUserId,
        Long journalEntryId,
        List<InvoiceLineDto> lines
) {}

