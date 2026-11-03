package com.backend.finledger.company.service.impl;

import com.backend.finledger.common.BusinessException;
import com.backend.finledger.company.dto.CompanyDto;
import com.backend.finledger.company.dto.CompanyUpdateRequest;
import com.backend.finledger.company.entity.Company;
import com.backend.finledger.company.mapper.CompanyMapper;
import com.backend.finledger.company.repository.CompanyRepository;
import com.backend.finledger.company.service.CompanyService;
import com.backend.finledger.security.CurrentUser;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CompanyServiceImpl implements CompanyService {

    private final CompanyRepository companyRepository;
    private final CompanyMapper companyMapper;

    public CompanyServiceImpl(CompanyRepository companyRepository, CompanyMapper companyMapper) {
        this.companyRepository = companyRepository;
        this.companyMapper = companyMapper;
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('SETTINGS_EDIT') or hasAuthority('COMPANY_VIEW') or isAuthenticated()")
    public CompanyDto getCurrentCompany() {
        Long companyId = CurrentUser.getCompanyId();
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new BusinessException("COMPANY_NOT_FOUND", "Company not found for id: " + companyId));
        return companyMapper.toDto(company);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('SETTINGS_EDIT') or hasAuthority('COMPANY_EDIT')")
    public CompanyDto updateCurrentCompany(CompanyUpdateRequest request) {
        Long companyId = CurrentUser.getCompanyId();
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new BusinessException("COMPANY_NOT_FOUND", "Company not found for id: " + companyId));

        companyMapper.updateEntity(company, request);
        Company saved = companyRepository.save(company);
        return companyMapper.toDto(saved);
    }
}

