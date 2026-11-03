package com.backend.finledger.purchases.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record OpenBillDto(
        Long id,
        String billNo,
        String vendorInvoiceNo,
        LocalDate billDate,
        LocalDate dueDate,
        BigDecimal total,
        BigDecimal amountPaid,
        BigDecimal balanceDue
) {}

