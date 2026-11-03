package com.backend.finledger.currency.service.impl;

import com.backend.finledger.common.BusinessException;
import com.backend.finledger.currency.dto.CurrencyDto;
import com.backend.finledger.currency.dto.ExchangeRateDto;
import com.backend.finledger.currency.dto.ExchangeRateRequest;
import com.backend.finledger.currency.entity.ExchangeRate;
import com.backend.finledger.currency.mapper.CurrencyMapper;
import com.backend.finledger.currency.repository.CurrencyRepository;
import com.backend.finledger.currency.repository.ExchangeRateRepository;
import com.backend.finledger.currency.service.CurrencyService;
import com.backend.finledger.security.CurrentUser;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CurrencyServiceImpl implements CurrencyService {

    private final CurrencyRepository currencyRepository;
    private final ExchangeRateRepository exchangeRateRepository;
    private final CurrencyMapper currencyMapper;

    public CurrencyServiceImpl(
            CurrencyRepository currencyRepository,
            ExchangeRateRepository exchangeRateRepository,
            CurrencyMapper currencyMapper) {
        this.currencyRepository = currencyRepository;
        this.exchangeRateRepository = exchangeRateRepository;
        this.currencyMapper = currencyMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CurrencyDto> getAllCurrencies() {
        return currencyRepository.findAll().stream()
                .map(currencyMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('SETTINGS_EDIT') or hasAuthority('CURRENCY_VIEW') or isAuthenticated()")
    public List<ExchangeRateDto> getExchangeRates() {
        Long companyId = CurrentUser.getCompanyId();
        return exchangeRateRepository.findByCompanyIdOrderByRateDateDesc(companyId).stream()
                .map(currencyMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('SETTINGS_EDIT') or hasAuthority('CURRENCY_EDIT')")
    public ExchangeRateDto setExchangeRate(ExchangeRateRequest request) {
        Long companyId = CurrentUser.getCompanyId();

        if (!currencyRepository.existsById(request.fromCurrency())) {
            throw new BusinessException("CURRENCY_NOT_FOUND", "Currency not found: " + request.fromCurrency());
        }
        if (!currencyRepository.existsById(request.toCurrency())) {
            throw new BusinessException("CURRENCY_NOT_FOUND", "Currency not found: " + request.toCurrency());
        }

        ExchangeRate rate = exchangeRateRepository.findByCompanyIdAndFromCurrencyAndToCurrencyAndRateDate(
                companyId, request.fromCurrency(), request.toCurrency(), request.rateDate())
                .orElseGet(() -> new ExchangeRate(
                        companyId,
                        request.fromCurrency(),
                        request.toCurrency(),
                        request.rateDate(),
                        request.rate()
                ));

        rate.setRate(request.rate());
        ExchangeRate saved = exchangeRateRepository.save(rate);
        return currencyMapper.toDto(saved);
    }
}

