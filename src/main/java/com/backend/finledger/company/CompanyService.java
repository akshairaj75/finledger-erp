package com.backend.finledger.company;

import com.backend.finledger.common.BusinessException;
import com.backend.finledger.company.dto.CompanyDto;
import com.backend.finledger.company.dto.CompanyUpdateRequest;
import com.backend.finledger.security.CurrentUser;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CompanyService {

    private final CompanyRepository companyRepository;
    private final CompanyMapper companyMapper;

    public CompanyService(CompanyRepository companyRepository, CompanyMapper companyMapper) {
        this.companyRepository = companyRepository;
        this.companyMapper = companyMapper;
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('SETTINGS_EDIT') or hasAuthority('COMPANY_VIEW') or isAuthenticated()")
    public CompanyDto getCurrentCompany() {
        Long companyId = CurrentUser.getCompanyId();
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new BusinessException("COMPANY_NOT_FOUND", "Company not found for id: " + companyId));
        return companyMapper.toDto(company);
    }

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

