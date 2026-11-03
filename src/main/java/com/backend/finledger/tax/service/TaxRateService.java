package com.backend.finledger.tax.service;

import com.backend.finledger.tax.dto.TaxRateDto;
import com.backend.finledger.tax.dto.TaxRateRequest;

import java.util.List;

public interface TaxRateService {
    List<TaxRateDto> getTaxRates();
    TaxRateDto getTaxRateById(Long id);
    TaxRateDto createTaxRate(TaxRateRequest request);
    TaxRateDto updateTaxRate(Long id, TaxRateRequest request);
}

