package com.backend.finledger.sales;

import com.backend.finledger.sales.dto.*;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface SalesMapper {

    CustomerDto toCustomerDto(Customer customer);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "companyId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "version", ignore = true)
    Customer toCustomerEntity(CustomerRequest request);

    InvoiceLineDto toInvoiceLineDto(InvoiceLine line);

    List<InvoiceLineDto> toInvoiceLineDtoList(List<InvoiceLine> lines);

    @Mapping(target = "customerName", ignore = true)
    @Mapping(target = "customerCode", ignore = true)
    InvoiceDto toInvoiceDto(Invoice invoice);

    OpenInvoiceDto toOpenInvoiceDto(Invoice invoice);

    List<OpenInvoiceDto> toOpenInvoiceDtoList(List<Invoice> invoices);

    @Mapping(target = "invoiceNo", ignore = true)
    ReceiptAllocationDto toReceiptAllocationDto(ReceiptAllocation allocation);

    List<ReceiptAllocationDto> toReceiptAllocationDtoList(List<ReceiptAllocation> allocations);

    @Mapping(target = "customerName", ignore = true)
    @Mapping(target = "customerCode", ignore = true)
    @Mapping(target = "bankAccountName", ignore = true)
    ReceiptDto toReceiptDto(Receipt receipt);
}
