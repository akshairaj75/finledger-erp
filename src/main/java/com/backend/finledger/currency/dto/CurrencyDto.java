package com.backend.finledger.currency.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CurrencyDto(
    String code,
    String name,
    String symbol,
    Byte decimalPlaces
) {}

