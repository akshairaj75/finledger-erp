package com.backend.finledger.tax.service.impl;

import com.backend.finledger.common.BusinessException;
import com.backend.finledger.security.CurrentUser;
import com.backend.finledger.tax.dto.TaxRateDto;
import com.backend.finledger.tax.dto.TaxRateRequest;
import com.backend.finledger.tax.entity.TaxRate;
import com.backend.finledger.tax.mapper.TaxRateMapper;
import com.backend.finledger.tax.repository.TaxRateRepository;
import com.backend.finledger.tax.service.TaxRateService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class TaxRateServiceImpl implements TaxRateService {

    private final TaxRateRepository taxRateRepository;
    private final TaxRateMapper taxRateMapper;

    public TaxRateServiceImpl(TaxRateRepository taxRateRepository, TaxRateMapper taxRateMapper) {
        this.taxRateRepository = taxRateRepository;
        this.taxRateMapper = taxRateMapper;
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('SETTINGS_EDIT') or hasAuthority('TAX_RATE_VIEW') or isAuthenticated()")
    public List<TaxRateDto> getTaxRates() {
        Long companyId = CurrentUser.getCompanyId();
        return taxRateRepository.findByCompanyId(companyId).stream()
                .map(taxRateMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('SETTINGS_EDIT') or hasAuthority('TAX_RATE_VIEW') or isAuthenticated()")
    public TaxRateDto getTaxRateById(Long id) {
        Long companyId = CurrentUser.getCompanyId();
        TaxRate taxRate = taxRateRepository.findByCompanyIdAndId(companyId, id)
                .orElseThrow(() -> new BusinessException("TAX_RATE_NOT_FOUND", "Tax rate not found with id: " + id));
        return taxRateMapper.toDto(taxRate);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('SETTINGS_EDIT') or hasAuthority('TAX_RATE_EDIT')")
    public TaxRateDto createTaxRate(TaxRateRequest request) {
        Long companyId = CurrentUser.getCompanyId();
        if (taxRateRepository.findByCompanyIdAndCode(companyId, request.code()).isPresent()) {
            throw new BusinessException("TAX_CODE_EXISTS", "Tax rate code already exists: " + request.code());
        }

        TaxRate taxRate = new TaxRate(
                companyId,
                request.code().trim().toUpperCase(),
                request.name().trim(),
                request.rate(),
                request.taxType()
        );
        taxRate.setSalesAccountId(request.salesAccountId());
        taxRate.setPurchaseAccountId(request.purchaseAccountId());
        taxRate.setIsActive(request.isActive() != null ? request.isActive() : true);

        TaxRate saved = taxRateRepository.save(taxRate);
        return taxRateMapper.toDto(saved);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('SETTINGS_EDIT') or hasAuthority('TAX_RATE_EDIT')")
    public TaxRateDto updateTaxRate(Long id, TaxRateRequest request) {
        Long companyId = CurrentUser.getCompanyId();
        TaxRate taxRate = taxRateRepository.findByCompanyIdAndId(companyId, id)
                .orElseThrow(() -> new BusinessException("TAX_RATE_NOT_FOUND", "Tax rate not found with id: " + id));

        taxRate.setName(request.name().trim());
        taxRate.setRate(request.rate());
        taxRate.setTaxType(request.taxType());
        taxRate.setSalesAccountId(request.salesAccountId());
        taxRate.setPurchaseAccountId(request.purchaseAccountId());
        if (request.isActive() != null) {
            taxRate.setIsActive(request.isActive());
        }

        TaxRate saved = taxRateRepository.save(taxRate);
        return taxRateMapper.toDto(saved);
    }
}

