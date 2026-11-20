package com.backend.finledger.company.mapper;

import com.backend.finledger.company.dto.CompanyDto;
import com.backend.finledger.company.dto.CompanyUpdateRequest;
import com.backend.finledger.company.entity.Company;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface CompanyMapper {

    @Mapping(target = "logoPath", source = "logoPath", qualifiedByName = "mapLogoPath")
    CompanyDto toDto(Company company);

    @Mapping(target = "logoPath", ignore = true)
    void updateEntity(@MappingTarget Company company, CompanyUpdateRequest request);

    @Named("mapLogoPath")
    default String mapLogoPath(String logoPath) {
        if (logoPath != null && !logoPath.isBlank()) {
            return "/api/v1/companies/current/logo";
        }
        return null;
    }
}
