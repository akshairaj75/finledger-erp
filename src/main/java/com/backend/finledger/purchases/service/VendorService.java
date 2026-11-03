package com.backend.finledger.purchases.service;

import com.backend.finledger.common.PageResponse;
import com.backend.finledger.purchases.dto.OpenBillDto;
import com.backend.finledger.purchases.dto.VendorDto;
import com.backend.finledger.purchases.dto.VendorRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface VendorService {

    VendorDto createVendor(VendorRequest request);

    VendorDto updateVendor(Long id, VendorRequest request);

    VendorDto getVendorById(Long id);

    PageResponse<VendorDto> getVendors(String search, Pageable pageable);

    List<OpenBillDto> getOpenBills(Long vendorId);
}

