package com.backend.finledger.sales.dto;

import com.backend.finledger.sales.entity.InvoiceDocType;

import java.math.BigDecimal;
import java.time.LocalDate;

public record OpenInvoiceDto(
        Long id,
        String invoiceNo,
        InvoiceDocType docType,
        LocalDate invoiceDate,
        LocalDate dueDate,
        String currencyCode,
        BigDecimal total,
        BigDecimal amountPaid,
        BigDecimal balanceDue
) {}

