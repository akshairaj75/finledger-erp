package com.backend.finledger.sales.dto;

import com.backend.finledger.sales.PaymentMethod;
import com.backend.finledger.sales.ReceiptStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ReceiptDto(
        Long id,
        Long companyId,
        String receiptNo,
        Long customerId,
        String customerName,
        String customerCode,
        LocalDate receiptDate,
        Long bankAccountId,
        String bankAccountName,
        PaymentMethod paymentMethod,
        String reference,
        String currencyCode,
        BigDecimal amount,
        BigDecimal unappliedAmount,
        ReceiptStatus status,
        Long journalEntryId,
        List<ReceiptAllocationDto> allocations
) {}

