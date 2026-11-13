package com.backend.finledger.purchases.service;

import com.backend.finledger.common.PageResponse;
import com.backend.finledger.purchases.dto.BillDto;
import com.backend.finledger.purchases.dto.BillRequest;
import com.backend.finledger.purchases.entity.BillStatus;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

public interface BillService {

    BillDto createBill(BillRequest request);

    BillDto updateBill(Long id, BillRequest request);

    BillDto getBillById(Long id);

    PageResponse<BillDto> getBills(Long vendorId, Pageable pageable);

    PageResponse<BillDto> getBills(
            Long vendorId,
            BillStatus status,
            LocalDate from,
            LocalDate to,
            String q,
            Pageable pageable);

    BillDto approveBill(Long id);

    BillDto voidBill(Long id, String reason);
}

