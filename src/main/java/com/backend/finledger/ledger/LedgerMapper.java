package com.backend.finledger.ledger;

import com.backend.finledger.ledger.dto.*;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface LedgerMapper {

    FiscalPeriodDto toPeriodDto(FiscalPeriod entity);
    List<FiscalPeriodDto> toPeriodDtoList(List<FiscalPeriod> entities);

    FiscalYearDto toYearDto(FiscalYear entity);
    List<FiscalYearDto> toYearDtoList(List<FiscalYear> entities);

    CostCenterDto toCostCenterDto(CostCenter entity);
    List<CostCenterDto> toCostCenterDtoList(List<CostCenter> entities);

    @Mapping(target = "accountCode", source = "account.code")
    @Mapping(target = "accountName", source = "account.name")
    JournalLineDto toLineDto(JournalLine entity);
    List<JournalLineDto> toLineDtoList(List<JournalLine> entities);

    JournalEntryDto toEntryDto(JournalEntry entity);
}
