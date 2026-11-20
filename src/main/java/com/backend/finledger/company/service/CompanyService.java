package com.backend.finledger.company.service;

import com.backend.finledger.company.dto.CompanyDto;
import com.backend.finledger.company.dto.CompanyUpdateRequest;
import com.backend.finledger.company.dto.LogoResource;
import org.springframework.web.multipart.MultipartFile;

public interface CompanyService {
    CompanyDto getCurrentCompany();
    CompanyDto updateCurrentCompany(CompanyUpdateRequest request, MultipartFile logoFile);
    LogoResource getCurrentCompanyLogo();
}
