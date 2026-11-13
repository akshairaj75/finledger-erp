package com.backend.finledger.sales.service;

import com.backend.finledger.common.PageResponse;
import com.backend.finledger.sales.dto.InvoiceDto;
import com.backend.finledger.sales.dto.InvoiceRequest;
import com.backend.finledger.sales.entity.InvoiceDocType;
import com.backend.finledger.sales.entity.InvoiceStatus;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

public interface InvoiceService {

    InvoiceDto createInvoice(InvoiceRequest request);

    InvoiceDto updateInvoice(Long id, InvoiceRequest request);

    InvoiceDto getInvoiceById(Long id);

    PageResponse<InvoiceDto> getInvoices(Long customerId, Pageable pageable);

    PageResponse<InvoiceDto> getInvoices(
            Long customerId,
            InvoiceStatus status,
            InvoiceDocType docType,
            LocalDate from,
            LocalDate to,
            String q,
            Pageable pageable);

    InvoiceDto sendInvoice(Long id);

    InvoiceDto voidInvoice(Long id, String reason);

    byte[] generateInvoicePdf(Long id);
}

