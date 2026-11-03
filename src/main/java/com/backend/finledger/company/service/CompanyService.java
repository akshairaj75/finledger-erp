package com.backend.finledger.company.service;

import com.backend.finledger.company.dto.CompanyDto;
import com.backend.finledger.company.dto.CompanyUpdateRequest;

public interface CompanyService {
    CompanyDto getCurrentCompany();
    CompanyDto updateCurrentCompany(CompanyUpdateRequest request);
}

