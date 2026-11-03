package com.backend.finledger.company.controller;

import com.backend.finledger.company.dto.CompanyDto;
import com.backend.finledger.company.dto.CompanyUpdateRequest;
import com.backend.finledger.company.service.CompanyService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/companies")
public class CompanyController {

    private final CompanyService companyService;

    public CompanyController(CompanyService companyService) {
        this.companyService = companyService;
    }

    @GetMapping("/current")
    public ResponseEntity<CompanyDto> getCurrent() {
        return ResponseEntity.ok(companyService.getCurrentCompany());
    }

    @PutMapping("/current")
    public ResponseEntity<CompanyDto> updateCurrent(@Valid @RequestBody CompanyUpdateRequest request) {
        return ResponseEntity.ok(companyService.updateCurrentCompany(request));
    }
}

