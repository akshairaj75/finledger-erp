package com.backend.finledger.ledger.service.impl;

import com.backend.finledger.common.BusinessException;
import com.backend.finledger.ledger.dto.CreateFiscalYearRequest;
import com.backend.finledger.ledger.dto.FiscalPeriodDto;
import com.backend.finledger.ledger.dto.FiscalYearDto;
import com.backend.finledger.ledger.entity.FiscalPeriod;
import com.backend.finledger.ledger.entity.FiscalPeriodStatus;
import com.backend.finledger.ledger.entity.FiscalYear;
import com.backend.finledger.ledger.mapper.LedgerMapper;
import com.backend.finledger.ledger.repository.FiscalPeriodRepository;
import com.backend.finledger.ledger.repository.FiscalYearRepository;
import com.backend.finledger.ledger.service.FiscalYearPeriodService;
import com.backend.finledger.security.CurrentUser;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class FiscalYearPeriodServiceImpl implements FiscalYearPeriodService {

    private final FiscalYearRepository fiscalYearRepository;
    private final FiscalPeriodRepository fiscalPeriodRepository;
    private final LedgerMapper ledgerMapper;

    public FiscalYearPeriodServiceImpl(
            FiscalYearRepository fiscalYearRepository,
            FiscalPeriodRepository fiscalPeriodRepository,
            LedgerMapper ledgerMapper) {
        this.fiscalYearRepository = fiscalYearRepository;
        this.fiscalPeriodRepository = fiscalPeriodRepository;
        this.ledgerMapper = ledgerMapper;
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('SETTINGS_VIEW') or hasAuthority('SETTINGS_EDIT') or hasAuthority('ACCOUNT_VIEW') or isAuthenticated()")
    public List<FiscalYearDto> getFiscalYears() {
        Long companyId = CurrentUser.getCompanyId();
        List<FiscalYear> years = fiscalYearRepository.findByCompanyIdOrderByStartDateDesc(companyId);
        return ledgerMapper.toYearDtoList(years);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('SETTINGS_EDIT')")
    public FiscalYearDto createFiscalYear(CreateFiscalYearRequest request) {
        Long companyId = CurrentUser.getCompanyId();

        if (fiscalYearRepository.findByCompanyIdAndName(companyId, request.name()).isPresent()) {
            throw new BusinessException("FISCAL_YEAR_EXISTS", "Fiscal year with name already exists: " + request.name());
        }

        if (request.endDate().isBefore(request.startDate())) {
            throw new BusinessException("INVALID_DATE_RANGE", "End date cannot be before start date");
        }

        FiscalYear fy = new FiscalYear(companyId, request.name(), request.startDate(), request.endDate());
        fy = fiscalYearRepository.save(fy);

        boolean autoGen = request.autoGeneratePeriods() == null || request.autoGeneratePeriods();
        if (autoGen) {
            LocalDate current = request.startDate();
            byte periodNo = 1;
            while (!current.isAfter(request.endDate()) && periodNo <= 12) {
                LocalDate periodEnd = current.withDayOfMonth(current.lengthOfMonth());
                if (periodEnd.isAfter(request.endDate())) {
                    periodEnd = request.endDate();
                }

                FiscalPeriod period = new FiscalPeriod(fy.getId(), periodNo, current, periodEnd);
                fy.getPeriods().add(fiscalPeriodRepository.save(period));

                current = periodEnd.plusDays(1);
                periodNo++;
            }
        }

        return ledgerMapper.toYearDto(fy);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('SETTINGS_EDIT') or hasAuthority('ACCOUNT_EDIT')")
    public FiscalPeriodDto closePeriod(Long periodId) {
        Long companyId = CurrentUser.getCompanyId();

        FiscalPeriod period = fiscalPeriodRepository.findByCompanyIdAndId(companyId, periodId)
                .orElseThrow(() -> new BusinessException("PERIOD_NOT_FOUND", "Fiscal period not found with id: " + periodId));

        period.setStatus(FiscalPeriodStatus.CLOSED);
        FiscalPeriod saved = fiscalPeriodRepository.save(period);
        return ledgerMapper.toPeriodDto(saved);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('SETTINGS_EDIT')")
    public FiscalPeriodDto lockPeriod(Long periodId) {
        Long companyId = CurrentUser.getCompanyId();

        FiscalPeriod period = fiscalPeriodRepository.findByCompanyIdAndId(companyId, periodId)
                .orElseThrow(() -> new BusinessException("PERIOD_NOT_FOUND", "Fiscal period not found with id: " + periodId));

        period.setStatus(FiscalPeriodStatus.LOCKED);
        FiscalPeriod saved = fiscalPeriodRepository.save(period);
        return ledgerMapper.toPeriodDto(saved);
    }
}

