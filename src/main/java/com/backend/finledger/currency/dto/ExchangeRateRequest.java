package com.backend.finledger.currency.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

public record ExchangeRateRequest(
    @NotBlank(message = "From currency is required")
    String fromCurrency,

    @NotBlank(message = "To currency is required")
    String toCurrency,

    @NotNull(message = "Rate date is required")
    LocalDate rateDate,

    @NotNull(message = "Rate is required")
    @DecimalMin(value = "0.000001", message = "Rate must be positive")
    BigDecimal rate
) {}

