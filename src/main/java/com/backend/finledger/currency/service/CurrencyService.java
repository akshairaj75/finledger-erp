package com.backend.finledger.currency.service;

import com.backend.finledger.currency.dto.CurrencyDto;
import com.backend.finledger.currency.dto.ExchangeRateDto;
import com.backend.finledger.currency.dto.ExchangeRateRequest;

import java.util.List;

public interface CurrencyService {
    List<CurrencyDto> getAllCurrencies();
    List<ExchangeRateDto> getExchangeRates();
    ExchangeRateDto setExchangeRate(ExchangeRateRequest request);
}

