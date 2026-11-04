package com.backend.finledger.crm.dto;

import com.backend.finledger.crm.entity.QuotationStatus;

public record ConvertToInvoiceResponse(
    Long quotationId,
    String quotationNo,
    Long invoiceId,
    String invoiceNo,
    QuotationStatus status
) {}

