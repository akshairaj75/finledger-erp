package com.backend.finledger.ledger.service;

import com.backend.finledger.ledger.dto.CostCenterDto;
import com.backend.finledger.ledger.dto.CostCenterRequest;

import java.util.List;

public interface CostCenterService {
    List<CostCenterDto> getCostCenters(Boolean activeOnly);
    CostCenterDto getCostCenterById(Long id);
    CostCenterDto createCostCenter(CostCenterRequest request);
    CostCenterDto updateCostCenter(Long id, CostCenterRequest request);
}

