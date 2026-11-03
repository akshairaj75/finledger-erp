package com.backend.finledger.purchases.mapper;

import com.backend.finledger.purchases.dto.*;
import com.backend.finledger.purchases.entity.*;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface PurchasesMapper {

    VendorDto toVendorDto(Vendor vendor);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "companyId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "version", ignore = true)
    Vendor toVendorEntity(VendorRequest request);

    BillLineDto toBillLineDto(BillLine line);

    List<BillLineDto> toBillLineDtoList(List<BillLine> lines);

    @Mapping(target = "vendorName", ignore = true)
    @Mapping(target = "vendorCode", ignore = true)
    BillDto toBillDto(Bill bill);

    OpenBillDto toOpenBillDto(Bill bill);

    List<OpenBillDto> toOpenBillDtoList(List<Bill> bills);

    @Mapping(target = "billNo", ignore = true)
    PaymentAllocationDto toPaymentAllocationDto(PaymentAllocation allocation);

    List<PaymentAllocationDto> toPaymentAllocationDtoList(List<PaymentAllocation> allocations);

    @Mapping(target = "vendorName", ignore = true)
    @Mapping(target = "vendorCode", ignore = true)
    @Mapping(target = "bankAccountName", ignore = true)
    @Mapping(target = "unappliedAmount", ignore = true)
    PaymentDto toPaymentDto(Payment payment);
}

