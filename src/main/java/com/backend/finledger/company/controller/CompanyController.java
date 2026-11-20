package com.backend.finledger.company.controller;

import com.backend.finledger.company.dto.CompanyDto;
import com.backend.finledger.company.dto.CompanyUpdateRequest;
import com.backend.finledger.company.dto.LogoResource;
import com.backend.finledger.company.service.CompanyService;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/companies")
public class CompanyController {

    private final CompanyService companyService;

    public CompanyController(CompanyService companyService) {
        this.companyService = companyService;
    }

    @GetMapping("/current")
    @PreAuthorize("hasAnyAuthority('SETTINGS_EDIT','SETTINGS_VIEW','COMPANY_VIEW')")
    public ResponseEntity<CompanyDto> getCurrent() {
        return ResponseEntity.ok(companyService.getCurrentCompany());
    }

    @PutMapping(
        value = "/current",
        consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @PreAuthorize("hasAuthority('SETTINGS_EDIT') or hasAuthority('COMPANY_EDIT')")
    public ResponseEntity<CompanyDto> updateCurrent(
        @Valid @RequestPart("data") CompanyUpdateRequest request,
        @RequestPart(value = "logoFile", required = false) MultipartFile logoFile
    ) {
        return ResponseEntity.ok(companyService.updateCurrentCompany(request, logoFile));
    }

    @PutMapping(
        value = "/current",
        consumes = MediaType.APPLICATION_JSON_VALUE
    )
    @PreAuthorize("hasAuthority('SETTINGS_EDIT') or hasAuthority('COMPANY_EDIT')")
    public ResponseEntity<CompanyDto> updateCurrentJson(
        @Valid @RequestBody CompanyUpdateRequest request
    ) {
        return ResponseEntity.ok(companyService.updateCurrentCompany(request, null));
    }

    @GetMapping("/current/logo")
    @PreAuthorize("hasAnyAuthority('SETTINGS_EDIT','SETTINGS_VIEW','COMPANY_VIEW')")
    public ResponseEntity<Resource> getCurrentLogo() {
        LogoResource logo = companyService.getCurrentCompanyLogo();
        if (logo == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(logo.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + logo.filename() + "\"")
                .header("X-Content-Type-Options", "nosniff")
                .header(HttpHeaders.CACHE_CONTROL, "private, max-age=3600")
                .body(logo.resource());
    }
}
