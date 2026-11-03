package com.backend.finledger.sales.dto;

import com.backend.finledger.sales.entity.InvoiceDocType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record InvoiceRequest(
        InvoiceDocType docType,

        @NotNull(message = "Customer ID is required")
        Long customerId,

        @NotNull(message = "Invoice date is required")
        LocalDate invoiceDate,

        LocalDate dueDate,

        String currencyCode,
        BigDecimal exchangeRate,
        String notes,
        Long quotationId,
        Long salespersonUserId,

        @NotEmpty(message = "Invoice must contain at least one line")
        @Valid
        List<InvoiceLineRequest> lines
) {}

