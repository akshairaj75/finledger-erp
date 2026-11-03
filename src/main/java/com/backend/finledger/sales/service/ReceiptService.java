package com.backend.finledger.sales.service;

import com.backend.finledger.common.PageResponse;
import com.backend.finledger.sales.dto.ReceiptDto;
import com.backend.finledger.sales.dto.ReceiptRequest;
import org.springframework.data.domain.Pageable;

public interface ReceiptService {

    ReceiptDto createReceipt(ReceiptRequest request);

    ReceiptDto voidReceipt(Long id, String reason);

    ReceiptDto getReceiptById(Long id);

    PageResponse<ReceiptDto> getReceipts(Long customerId, Pageable pageable);
}

