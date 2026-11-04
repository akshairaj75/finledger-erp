package com.backend.finledger.crm.service;

import com.backend.finledger.common.PageResponse;
import com.backend.finledger.crm.dto.ConvertToInvoiceResponse;
import com.backend.finledger.crm.dto.QuotationDto;
import com.backend.finledger.crm.dto.QuotationRequest;
import com.backend.finledger.crm.entity.QuotationStatus;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

public interface QuotationService {
    PageResponse<QuotationDto> getQuotations(Long customerId, Long opportunityId, QuotationStatus status,
                                             LocalDate fromDate, LocalDate toDate, String search, Pageable pageable);
    QuotationDto getQuotationById(Long id);
    QuotationDto createQuotation(QuotationRequest request);
    QuotationDto updateQuotation(Long id, QuotationRequest request);
    QuotationDto sendQuotation(Long id);
    QuotationDto acceptQuotation(Long id);
    ConvertToInvoiceResponse convertToInvoice(Long id);
    byte[] generateQuotationPdf(Long id);
}

