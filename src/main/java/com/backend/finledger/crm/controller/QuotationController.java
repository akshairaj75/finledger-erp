package com.backend.finledger.crm.controller;

import com.backend.finledger.common.PageResponse;
import com.backend.finledger.crm.dto.ConvertToInvoiceResponse;
import com.backend.finledger.crm.dto.QuotationDto;
import com.backend.finledger.crm.dto.QuotationRequest;
import com.backend.finledger.crm.entity.QuotationStatus;
import com.backend.finledger.crm.service.QuotationService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/quotations")
public class QuotationController {

    private final QuotationService quotationService;

    public QuotationController(QuotationService quotationService) {
        this.quotationService = quotationService;
    }

    @GetMapping
    public ResponseEntity<PageResponse<QuotationDto>> getQuotations(
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) Long opportunityId,
            @RequestParam(required = false) QuotationStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) String search,
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(quotationService.getQuotations(customerId, opportunityId, status, fromDate, toDate, search, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<QuotationDto> getQuotationById(@PathVariable Long id) {
        return ResponseEntity.ok(quotationService.getQuotationById(id));
    }

    @PostMapping
    public ResponseEntity<QuotationDto> createQuotation(@Valid @RequestBody QuotationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(quotationService.createQuotation(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<QuotationDto> updateQuotation(@PathVariable Long id, @Valid @RequestBody QuotationRequest request) {
        return ResponseEntity.ok(quotationService.updateQuotation(id, request));
    }

    @PostMapping("/{id}/send")
    public ResponseEntity<QuotationDto> sendQuotation(@PathVariable Long id) {
        return ResponseEntity.ok(quotationService.sendQuotation(id));
    }

    @PostMapping("/{id}/accept")
    public ResponseEntity<QuotationDto> acceptQuotation(@PathVariable Long id) {
        return ResponseEntity.ok(quotationService.acceptQuotation(id));
    }

    @PostMapping("/{id}/convert-to-invoice")
    public ResponseEntity<ConvertToInvoiceResponse> convertToInvoice(@PathVariable Long id) {
        return ResponseEntity.ok(quotationService.convertToInvoice(id));
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> getQuotationPdf(@PathVariable Long id) {
        byte[] pdf = quotationService.generateQuotationPdf(id);
        QuotationDto quotation = quotationService.getQuotationById(id);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("inline", "quotation-" + quotation.quotationNo() + ".pdf");
        headers.setContentLength(pdf.length);

        return new ResponseEntity<>(pdf, headers, HttpStatus.OK);
    }
}

