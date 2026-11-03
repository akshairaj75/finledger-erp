package com.backend.finledger.tax.mapper;

import com.backend.finledger.tax.dto.TaxRateDto;
import com.backend.finledger.tax.entity.TaxRate;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface TaxRateMapper {
    TaxRateDto toDto(TaxRate taxRate);
}

