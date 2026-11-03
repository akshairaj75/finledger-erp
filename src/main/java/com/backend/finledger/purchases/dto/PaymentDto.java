package com.backend.finledger.purchases.dto;

import com.backend.finledger.purchases.entity.PaymentStatus;
import com.backend.finledger.sales.entity.PaymentMethod;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record PaymentDto(
        Long id,
        Long companyId,
        String paymentNo,
        Long vendorId,
        String vendorName,
        String vendorCode,
        LocalDate paymentDate,
        Long bankAccountId,
        String bankAccountName,
        PaymentMethod paymentMethod,
        String reference,
        String currencyCode,
        BigDecimal amount,
        BigDecimal unappliedAmount,
        PaymentStatus status,
        Long journalEntryId,
        List<PaymentAllocationDto> allocations
) {}

