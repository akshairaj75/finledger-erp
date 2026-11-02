package com.backend.finledger.currency.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

public record ExchangeRateDto(
    Long id,
    Long companyId,
    String fromCurrency,
    String toCurrency,
    LocalDate rateDate,
    BigDecimal rate
) {}

