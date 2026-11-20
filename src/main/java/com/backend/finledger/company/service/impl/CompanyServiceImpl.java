package com.backend.finledger.company.service.impl;

import com.backend.finledger.attachment.service.FileStorageService;
import com.backend.finledger.common.BusinessException;
import com.backend.finledger.company.dto.CompanyDto;
import com.backend.finledger.company.dto.CompanyUpdateRequest;
import com.backend.finledger.company.dto.LogoResource;
import com.backend.finledger.company.entity.Company;
import com.backend.finledger.company.mapper.CompanyMapper;
import com.backend.finledger.company.repository.CompanyRepository;
import com.backend.finledger.company.service.CompanyService;
import com.backend.finledger.security.CurrentUser;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class CompanyServiceImpl implements CompanyService {

    private static final long MAX_LOGO_SIZE = 2L * 1024 * 1024; // 2MB limit

    private final CompanyRepository companyRepository;
    private final CompanyMapper companyMapper;
    private final FileStorageService fileStorageService;

    public CompanyServiceImpl(
            CompanyRepository companyRepository,
            CompanyMapper companyMapper,
            FileStorageService fileStorageService
    ) {
        this.companyRepository = companyRepository;
        this.companyMapper = companyMapper;
        this.fileStorageService = fileStorageService;
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyAuthority('SETTINGS_EDIT','SETTINGS_VIEW','COMPANY_VIEW')")
    public CompanyDto getCurrentCompany() {
        Long companyId = CurrentUser.getCompanyId();
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new BusinessException("COMPANY_NOT_FOUND", "Company not found for id: " + companyId));
        return companyMapper.toDto(company);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('SETTINGS_EDIT') or hasAuthority('COMPANY_EDIT')")
    public CompanyDto updateCurrentCompany(CompanyUpdateRequest request, MultipartFile logoFile) {
        Long companyId = CurrentUser.getCompanyId();
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new BusinessException("COMPANY_NOT_FOUND", "Company not found for id: " + companyId));

        // 1. Conflict validation: cannot upload a new logo and request removal at the same time
        if (Boolean.TRUE.equals(request.removeLogo()) && logoFile != null && !logoFile.isEmpty()) {
            throw new BusinessException("INVALID_REQUEST", "Cannot simultaneously upload a new logo and request logo removal.");
        }

        // 2. Update company details
        companyMapper.updateEntity(company, request);

        // 3. Handle logo file upload or removal
        if (logoFile != null && !logoFile.isEmpty()) {
            validateLogoFile(logoFile);

            // Delete existing logo file if present
            if (company.getLogoPath() != null && !company.getLogoPath().isBlank()) {
                fileStorageService.delete(company.getLogoPath());
            }

            // Save new logo file using the standard file storage service
            String storedPath = fileStorageService.store(companyId, "COMPANY", companyId, logoFile);
            company.setLogoPath(storedPath);

        } else if (Boolean.TRUE.equals(request.removeLogo())) {
            // Delete existing logo file
            if (company.getLogoPath() != null && !company.getLogoPath().isBlank()) {
                fileStorageService.delete(company.getLogoPath());
            }
            company.setLogoPath(null);
        }
        // If neither: keep existing company.logoPath unchanged

        Company saved = companyRepository.save(company);
        return companyMapper.toDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyAuthority('SETTINGS_EDIT','SETTINGS_VIEW','COMPANY_VIEW')")
    public LogoResource getCurrentCompanyLogo() {
        Long companyId = CurrentUser.getCompanyId();
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new BusinessException("COMPANY_NOT_FOUND", "Company not found for id: " + companyId));

        String logoPath = company.getLogoPath();
        if (logoPath == null || logoPath.isBlank()) {
            return null;
        }

        Resource resource = fileStorageService.loadAsResource(logoPath);
        if (!resource.exists() || !resource.isReadable()) {
            return null;
        }

        String lower = logoPath.toLowerCase();
        String contentType = lower.endsWith(".png") ? MediaType.IMAGE_PNG_VALUE : MediaType.IMAGE_JPEG_VALUE;
        String filename = "logo." + (lower.endsWith(".png") ? "png" : "jpeg");

        return new LogoResource(resource, contentType, filename);
    }

    private void validateLogoFile(MultipartFile file) {
        if (file.getSize() > MAX_LOGO_SIZE) {
            throw new BusinessException("FILE_TOO_LARGE", "Logo file exceeds maximum permitted size of 2MB.");
        }

        String contentType = file.getContentType();
        String filename = file.getOriginalFilename();

        boolean isPng = (contentType != null && contentType.equalsIgnoreCase(MediaType.IMAGE_PNG_VALUE))
                || (filename != null && filename.toLowerCase().endsWith(".png"));
        boolean isJpeg = (contentType != null && (contentType.equalsIgnoreCase(MediaType.IMAGE_JPEG_VALUE) || contentType.equalsIgnoreCase("image/jpg")))
                || (filename != null && (filename.toLowerCase().endsWith(".jpg") || filename.toLowerCase().endsWith(".jpeg")));

        if (!isPng && !isJpeg) {
            throw new BusinessException("INVALID_FILE_TYPE", "Unsupported image format. Only PNG and JPEG images are allowed.");
        }
    }
}
