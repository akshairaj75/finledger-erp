package com.backend.finledger.sales.controller;

import com.backend.finledger.common.PageResponse;
import com.backend.finledger.common.VoidRequest;
import com.backend.finledger.sales.dto.InvoiceDto;
import com.backend.finledger.sales.dto.InvoiceRequest;
import com.backend.finledger.sales.entity.InvoiceDocType;
import com.backend.finledger.sales.entity.InvoiceStatus;
import com.backend.finledger.sales.service.InvoiceService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/invoices")
public class InvoiceController {

    private final InvoiceService invoiceService;

    public InvoiceController(InvoiceService invoiceService) {
        this.invoiceService = invoiceService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('INVOICE_CREATE')")
    public ResponseEntity<InvoiceDto> createInvoice(@Valid @RequestBody InvoiceRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(invoiceService.createInvoice(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('INVOICE_CREATE')")
    public ResponseEntity<InvoiceDto> updateInvoice(@PathVariable Long id, @Valid @RequestBody InvoiceRequest request) {
        return ResponseEntity.ok(invoiceService.updateInvoice(id, request));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('INVOICE_VIEW')")
    public ResponseEntity<InvoiceDto> getInvoice(@PathVariable Long id) {
        return ResponseEntity.ok(invoiceService.getInvoiceById(id));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('INVOICE_VIEW')")
    public ResponseEntity<PageResponse<InvoiceDto>> getInvoices(
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) InvoiceStatus status,
            @RequestParam(required = false) InvoiceDocType docType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String search,
            Pageable pageable) {
        String effectiveQuery = (q != null && !q.isBlank()) ? q : search;
        return ResponseEntity.ok(invoiceService.getInvoices(customerId, status, docType, from, to, effectiveQuery, pageable));
    }

    @PostMapping("/{id}/send")
    @PreAuthorize("hasAuthority('INVOICE_SEND')")
    public ResponseEntity<InvoiceDto> sendInvoice(@PathVariable Long id) {
        return ResponseEntity.ok(invoiceService.sendInvoice(id));
    }

    @PostMapping("/{id}/void")
    @PreAuthorize("hasAuthority('INVOICE_VOID')")
    public ResponseEntity<InvoiceDto> voidInvoice(
            @PathVariable Long id,
            @RequestParam(required = false) String reason,
            @RequestBody(required = false) VoidRequest body) {
        return ResponseEntity.ok(invoiceService.voidInvoice(id, VoidRequest.resolveReason(body, reason)));
    }

    @GetMapping("/{id}/pdf")
    @PreAuthorize("hasAuthority('INVOICE_VIEW')")
    public ResponseEntity<byte[]> getInvoicePdf(@PathVariable Long id) {
        byte[] pdfBytes = invoiceService.generateInvoicePdf(id);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("inline", "invoice-" + id + ".pdf");
        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }
}

