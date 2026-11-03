package com.backend.finledger.purchases.dto;

import com.backend.finledger.purchases.entity.BillDocType;
import com.backend.finledger.purchases.entity.BillStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record BillDto(
        Long id,
        Long companyId,
        String billNo,
        String vendorInvoiceNo,
        BillDocType docType,
        Long vendorId,
        String vendorName,
        String vendorCode,
        LocalDate billDate,
        LocalDate dueDate,
        String currencyCode,
        BigDecimal exchangeRate,
        BigDecimal subtotal,
        BigDecimal taxTotal,
        BigDecimal total,
        BigDecimal amountPaid,
        BigDecimal balanceDue,
        BillStatus status,
        Long journalEntryId,
        String notes,
        List<BillLineDto> lines
) {}

