package com.backend.finledger.sales.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CustomerRequest(
        @NotBlank(message = "Customer code is required")
        @Size(max = 20, message = "Code cannot exceed 20 characters")
        String code,

        @NotBlank(message = "Customer name is required")
        @Size(max = 20, message = "Name cannot exceed 200 characters")
        String name,

        String trn,
        String email,
        String phone,
        String billingAddress,
        String countryCode,

        @NotBlank(message = "Currency code is required")
        @Size(min = 3, max = 3, message = "Currency code must be 3 characters")
        String currencyCode,

        Short paymentTermsDays,
        BigDecimal creditLimit,

        @NotNull(message = "AR account ID is required")
        Long arAccountId,

        Long accountManagerId,
        Long sourceLeadId,
        Boolean isActive
) {}

