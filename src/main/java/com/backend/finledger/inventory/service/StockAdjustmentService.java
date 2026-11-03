package com.backend.finledger.inventory.service;

import com.backend.finledger.common.PageResponse;
import com.backend.finledger.inventory.dto.StockAdjustmentDto;
import com.backend.finledger.inventory.dto.StockAdjustmentRequest;
import org.springframework.data.domain.Pageable;

public interface StockAdjustmentService {
    PageResponse<StockAdjustmentDto> getAdjustments(Pageable pageable);
    StockAdjustmentDto getAdjustmentById(Long id);
    StockAdjustmentDto createDraftAdjustment(StockAdjustmentRequest request);
    StockAdjustmentDto postAdjustment(Long id);
    StockAdjustmentDto voidAdjustment(Long id, String reason);
}

