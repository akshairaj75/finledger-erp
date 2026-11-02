package com.backend.finledger.sales.dto;

import java.math.BigDecimal;

public record ReceiptAllocationDto(
        Long id,
        Long invoiceId,
        String invoiceNo,
        BigDecimal amount
) {}

