package com.backend.finledger.crm.dto;

import com.backend.finledger.crm.entity.QuotationStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record QuotationDto(
    Long id,
    Long companyId,
    String quotationNo,
    Long opportunityId,
    String opportunityTitle,
    Long customerId,
    String customerName,
    Long leadId,
    String leadNo,
    LocalDate quoteDate,
    LocalDate validUntil,
    String currencyCode,
    BigDecimal subtotal,
    BigDecimal taxTotal,
    BigDecimal total,
    QuotationStatus status,
    Long invoiceId,
    String invoiceNo,
    List<QuotationLineDto> lines,
    Instant createdAt
) {}

