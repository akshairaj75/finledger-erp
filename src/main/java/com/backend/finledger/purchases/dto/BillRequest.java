package com.backend.finledger.purchases.dto;

import com.backend.finledger.purchases.entity.BillDocType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record BillRequest(
        BillDocType docType,

        @Size(max = 50, message = "Vendor invoice number cannot exceed 50 characters")
        String vendorInvoiceNo,

        @NotNull(message = "Vendor ID is required")
        Long vendorId,

        @NotNull(message = "Bill date is required")
        LocalDate billDate,

        LocalDate dueDate,

        @Size(min = 3, max = 3, message = "Currency code must be 3 characters")
        String currencyCode,

        BigDecimal exchangeRate,
        String notes,

        @NotEmpty(message = "At least one bill line is required")
        @Valid
        List<BillLineRequest> lines
) {}

