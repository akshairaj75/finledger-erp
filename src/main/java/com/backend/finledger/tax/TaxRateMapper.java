package com.backend.finledger.tax;

import com.backend.finledger.tax.dto.TaxRateDto;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface TaxRateMapper {
    TaxRateDto toDto(TaxRate taxRate);
}

