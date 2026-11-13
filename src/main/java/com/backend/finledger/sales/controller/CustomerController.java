package com.backend.finledger.sales.controller;

import com.backend.finledger.common.PageResponse;
import com.backend.finledger.sales.dto.CustomerDto;
import com.backend.finledger.sales.dto.CustomerRequest;
import com.backend.finledger.sales.dto.CustomerStatementDto;
import com.backend.finledger.sales.dto.OpenInvoiceDto;
import com.backend.finledger.sales.service.CustomerService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/customers")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('CUSTOMER_CREATE')")
    public ResponseEntity<CustomerDto> createCustomer(@Valid @RequestBody CustomerRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(customerService.createCustomer(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('CUSTOMER_EDIT')")
    public ResponseEntity<CustomerDto> updateCustomer(@PathVariable Long id,
            @Valid @RequestBody CustomerRequest request) {
        return ResponseEntity.ok(customerService.updateCustomer(id, request));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('CUSTOMER_VIEW')")
    public ResponseEntity<CustomerDto> getCustomer(@PathVariable Long id) {
        return ResponseEntity.ok(customerService.getCustomerById(id));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('CUSTOMER_VIEW')")
    public ResponseEntity<PageResponse<CustomerDto>> getCustomers(
            @RequestParam(required = false) String search,
            Pageable pageable) {
        return ResponseEntity.ok(customerService.getCustomers(search, pageable));
    }

    @GetMapping("/{id}/open-invoices")
    @PreAuthorize("hasAuthority('INVOICE_VIEW')")
    public ResponseEntity<List<OpenInvoiceDto>> getOpenInvoices(@PathVariable Long id) {
        return ResponseEntity.ok(customerService.getOpenInvoices(id));
    }

    @GetMapping("/{id}/statement")
    @PreAuthorize("hasAuthority('CUSTOMER_VIEW')")
    public ResponseEntity<CustomerStatementDto> getCustomerStatement(
            @PathVariable Long id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        return ResponseEntity.ok(customerService.getCustomerStatement(id, fromDate, toDate));
    }
}
