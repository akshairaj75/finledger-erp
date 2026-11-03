package com.backend.finledger.purchases.dto;

import java.math.BigDecimal;

public record PaymentAllocationDto(
        Long id,
        Long billId,
        String billNo,
        BigDecimal amount
) {}

