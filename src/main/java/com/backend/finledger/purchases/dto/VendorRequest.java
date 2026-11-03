package com.backend.finledger.purchases.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record VendorRequest(
        @NotBlank(message = "Vendor code is required")
        @Size(max = 20, message = "Code cannot exceed 20 characters")
        String code,

        @NotBlank(message = "Vendor name is required")
        @Size(max = 200, message = "Name cannot exceed 200 characters")
        String name,

        String trn,
        String email,
        String phone,
        String address,

        @NotBlank(message = "Currency code is required")
        @Size(min = 3, max = 3, message = "Currency code must be 3 characters")
        String currencyCode,

        Short paymentTermsDays,
        String bankIban,

        @NotNull(message = "AP account ID is required")
        Long apAccountId,

        Boolean isActive
) {}

