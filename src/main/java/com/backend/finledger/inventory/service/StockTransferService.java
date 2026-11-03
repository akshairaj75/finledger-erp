package com.backend.finledger.inventory.service;

import com.backend.finledger.common.PageResponse;
import com.backend.finledger.inventory.dto.StockTransferDto;
import com.backend.finledger.inventory.dto.StockTransferRequest;
import org.springframework.data.domain.Pageable;

public interface StockTransferService {
    PageResponse<StockTransferDto> getTransfers(Pageable pageable);
    StockTransferDto getTransferById(Long id);
    StockTransferDto createDraftTransfer(StockTransferRequest request);
    StockTransferDto dispatchTransfer(Long id);
    StockTransferDto receiveTransfer(Long id);
    StockTransferDto voidTransfer(Long id, String reason);
}

