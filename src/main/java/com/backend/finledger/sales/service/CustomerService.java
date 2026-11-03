package com.backend.finledger.sales.service;

import com.backend.finledger.common.PageResponse;
import com.backend.finledger.sales.dto.CustomerDto;
import com.backend.finledger.sales.dto.CustomerRequest;
import com.backend.finledger.sales.dto.CustomerStatementDto;
import com.backend.finledger.sales.dto.OpenInvoiceDto;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

public interface CustomerService {

    CustomerDto createCustomer(CustomerRequest request);

    CustomerDto updateCustomer(Long id, CustomerRequest request);

    CustomerDto getCustomerById(Long id);

    PageResponse<CustomerDto> getCustomers(String search, Pageable pageable);

    List<OpenInvoiceDto> getOpenInvoices(Long customerId);

    CustomerStatementDto getCustomerStatement(Long customerId, LocalDate fromDate, LocalDate toDate);
}

