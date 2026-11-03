package com.backend.finledger.currency.mapper;

import com.backend.finledger.currency.dto.CurrencyDto;
import com.backend.finledger.currency.dto.ExchangeRateDto;
import com.backend.finledger.currency.entity.Currency;
import com.backend.finledger.currency.entity.ExchangeRate;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CurrencyMapper {
    CurrencyDto toDto(Currency currency);
    ExchangeRateDto toDto(ExchangeRate exchangeRate);
}

