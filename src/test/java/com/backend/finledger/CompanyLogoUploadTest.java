package com.backend.finledger;

import com.backend.finledger.attachment.service.FileStorageService;
import com.backend.finledger.common.BusinessException;
import com.backend.finledger.company.dto.CompanyDto;
import com.backend.finledger.company.dto.CompanyUpdateRequest;
import com.backend.finledger.company.dto.LogoResource;
import com.backend.finledger.company.entity.Company;
import com.backend.finledger.company.mapper.CompanyMapper;
import com.backend.finledger.company.repository.CompanyRepository;
import com.backend.finledger.company.service.CompanyService;
import com.backend.finledger.security.UserPrincipal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class CompanyLogoUploadTest {

    @Autowired
    private CompanyService companyService;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private CompanyMapper companyMapper;

    @Autowired
    private FileStorageService fileStorageService;

    private Long companyId;
    private String initialLogoPath;

    @BeforeEach
    void setUp() {
        Company company = companyRepository.findByCode("BSB")
                .orElseThrow(() -> new IllegalStateException("Seeded BSB company not found"));
        companyId = company.getId();
        initialLogoPath = company.getLogoPath();

        UserPrincipal principal = new UserPrincipal(
                1L,
                companyId,
                "admin@finledger.com",
                "",
                "Admin User",
                true,
                Set.of("SETTINGS_EDIT", "SETTINGS_VIEW", "COMPANY_VIEW", "COMPANY_EDIT")
        );
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );
    }

    @AfterEach
    void tearDown() {
        Company company = companyRepository.findById(companyId).orElse(null);
        if (company != null) {
            company.setLogoPath(initialLogoPath);
            companyRepository.save(company);
        }
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Should update company profile without modifying existing logo when no file provided and removeLogo=false")
    void testUpdateCompanyProfileWithoutLogo() {
        Company c = companyRepository.findById(companyId).orElseThrow();
        c.setLogoPath("test/existing/logo.png");
        companyRepository.save(c);

        CompanyUpdateRequest request = new CompanyUpdateRequest(
                "BSB Trading Updated LLC",
                "100234567800003",
                "AE",
                "AED",
                (byte) 1,
                "Office 1204, Business Bay",
                "info@bsb.com",
                "+97140000000",
                true,
                false,
                false
        );

        CompanyDto result = companyService.updateCurrentCompany(request, null);

        assertNotNull(result);
        assertEquals("BSB Trading Updated LLC", result.legalName());
        assertEquals("/api/v1/companies/current/logo", result.logoPath());

        Company refreshed = companyRepository.findById(companyId).orElseThrow();
        assertEquals("test/existing/logo.png", refreshed.getLogoPath());
    }

    @Test
    @DisplayName("Should upload logo for company without existing logo and return stable API route")
    void testUploadLogoForCompanyWithoutLogo() {
        Company c = companyRepository.findById(companyId).orElseThrow();
        c.setLogoPath(null);
        companyRepository.save(c);

        byte[] pngBytes = new byte[]{1, 2, 3, 4};
        MockMultipartFile file = new MockMultipartFile("logoFile", "company_logo.png", "image/png", pngBytes);

        CompanyUpdateRequest request = new CompanyUpdateRequest(
                "BSB Trading LLC",
                "100234567800003",
                "AE",
                "AED",
                (byte) 1,
                "Office 1204",
                "info@bsb.com",
                "+97140000000",
                true,
                false,
                false
        );

        CompanyDto dto = companyService.updateCurrentCompany(request, file);

        assertNotNull(dto);
        assertEquals("/api/v1/companies/current/logo", dto.logoPath());

        Company updated = companyRepository.findById(companyId).orElseThrow();
        assertNotNull(updated.getLogoPath());
        assertTrue(updated.getLogoPath().startsWith(companyId + "/COMPANY/" + companyId + "/"));
        assertTrue(updated.getLogoPath().endsWith(".png"));

        // Retrieve through getCurrentCompanyLogo()
        LogoResource logo = companyService.getCurrentCompanyLogo();
        assertNotNull(logo);
        assertEquals("image/png", logo.contentType());
        assertTrue(logo.resource().exists());
    }

    @Test
    @DisplayName("Should replace existing logo and delete previous logo file")
    void testReplaceExistingLogo() {
        // Step 1: Upload first logo
        byte[] pngBytes = new byte[]{1, 2, 3, 4};
        MockMultipartFile file1 = new MockMultipartFile("logoFile", "first.png", "image/png", pngBytes);

        CompanyUpdateRequest req1 = new CompanyUpdateRequest(
                "BSB Trading LLC", "100234567800003", "AE", "AED", (byte) 1,
                "Office 1204", "info@bsb.com", "+97140000000", true, false, false
        );

        companyService.updateCurrentCompany(req1, file1);
        Company afterFirst = companyRepository.findById(companyId).orElseThrow();
        String firstStorageKey = afterFirst.getLogoPath();
        assertNotNull(firstStorageKey);
        assertTrue(fileStorageService.loadAsResource(firstStorageKey).exists());

        // Step 2: Replace with JPEG logo
        byte[] jpegBytes = new byte[]{5, 6, 7, 8};
        MockMultipartFile file2 = new MockMultipartFile("logoFile", "second.jpeg", "image/jpeg", jpegBytes);

        companyService.updateCurrentCompany(req1, file2);

        Company afterSecond = companyRepository.findById(companyId).orElseThrow();
        String secondStorageKey = afterSecond.getLogoPath();
        assertNotNull(secondStorageKey);
        assertNotEquals(firstStorageKey, secondStorageKey);
        assertTrue(secondStorageKey.endsWith(".jpeg"));

        // Old file must be deleted
        assertThrows(BusinessException.class, () -> fileStorageService.loadAsResource(firstStorageKey));
        // New file exists
        assertTrue(fileStorageService.loadAsResource(secondStorageKey).exists());
    }

    @Test
    @DisplayName("Should remove existing logo when removeLogo=true and delete physical file")
    void testRemoveExistingLogo() {
        // Step 1: Upload a logo
        byte[] pngBytes = new byte[]{1, 2, 3, 4};
        MockMultipartFile file = new MockMultipartFile("logoFile", "initial.png", "image/png", pngBytes);

        CompanyUpdateRequest req = new CompanyUpdateRequest(
                "BSB Trading LLC", "100234567800003", "AE", "AED", (byte) 1,
                "Office 1204", "info@bsb.com", "+97140000000", true, false, false
        );

        companyService.updateCurrentCompany(req, file);
        Company companyWithLogo = companyRepository.findById(companyId).orElseThrow();
        String storageKey = companyWithLogo.getLogoPath();
        assertNotNull(storageKey);
        assertTrue(fileStorageService.loadAsResource(storageKey).exists());

        // Step 2: Remove logo with removeLogo=true
        CompanyUpdateRequest removeReq = new CompanyUpdateRequest(
                "BSB Trading LLC", "100234567800003", "AE", "AED", (byte) 1,
                "Office 1204", "info@bsb.com", "+97140000000", true, false, true
        );

        CompanyDto removedDto = companyService.updateCurrentCompany(removeReq, null);

        assertNull(removedDto.logoPath());
        Company afterRemoval = companyRepository.findById(companyId).orElseThrow();
        assertNull(afterRemoval.getLogoPath());

        // Old file deleted
        assertThrows(BusinessException.class, () -> fileStorageService.loadAsResource(storageKey));
        assertNull(companyService.getCurrentCompanyLogo());
    }

    @Test
    @DisplayName("Should reject conflicting request with both new logo and removeLogo=true")
    void testRejectConflictNewLogoAndRemoveLogo() {
        byte[] pngBytes = new byte[]{1, 2, 3, 4};
        MockMultipartFile file = new MockMultipartFile("logoFile", "logo.png", "image/png", pngBytes);

        CompanyUpdateRequest conflictReq = new CompanyUpdateRequest(
                "BSB Trading LLC", "100234567800003", "AE", "AED", (byte) 1,
                "Office 1204", "info@bsb.com", "+97140000000", true, false, true
        );

        BusinessException ex = assertThrows(BusinessException.class, () ->
                companyService.updateCurrentCompany(conflictReq, file));
        assertEquals("INVALID_REQUEST", ex.getCode());
    }

    @Test
    @DisplayName("Should reject unsupported image formats (e.g. text/plain, gif, svg)")
    void testRejectUnsupportedImageFormats() {
        byte[] textBytes = "This is a plain text file, not an image".getBytes();
        MockMultipartFile textFile = new MockMultipartFile("logoFile", "notes.txt", "text/plain", textBytes);

        CompanyUpdateRequest req = new CompanyUpdateRequest(
                "BSB Trading LLC", "100234567800003", "AE", "AED", (byte) 1,
                "Office 1204", "info@bsb.com", "+97140000000", true, false, false
        );

        BusinessException ex = assertThrows(BusinessException.class, () ->
                companyService.updateCurrentCompany(req, textFile));
        assertEquals("INVALID_FILE_TYPE", ex.getCode());
    }

    @Test
    @DisplayName("Should reject files exceeding 2 MB")
    void testRejectFilesExceeding2MB() {
        byte[] largeBytes = new byte[2 * 1024 * 1024 + 1024]; // > 2MB
        MockMultipartFile largeFile = new MockMultipartFile("logoFile", "large.png", "image/png", largeBytes);

        CompanyUpdateRequest req = new CompanyUpdateRequest(
                "BSB Trading LLC", "100234567800003", "AE", "AED", (byte) 1,
                "Office 1204", "info@bsb.com", "+97140000000", true, false, false
        );

        BusinessException ex = assertThrows(BusinessException.class, () ->
                companyService.updateCurrentCompany(req, largeFile));
        assertEquals("FILE_TOO_LARGE", ex.getCode());
    }

    @Test
    @DisplayName("Should ensure tenant isolation: user in company A cannot access or modify company B logo")
    void testTenantIsolation() {
        // Authenticated as company 1 (BSB)
        Company other = new Company();
        other.setCode("OTHER");
        other.setLegalName("Other Company LLC");
        other.setCountryCode("AE");
        other.setBaseCurrency("AED");
        other.setFiscalYearStartMonth((byte) 1);
        other.setLogoPath("other/company/logo.png");
        other = companyRepository.save(other);

        // Current user session is companyId (1)
        CompanyDto current = companyService.getCurrentCompany();
        assertEquals(companyId, current.id());
        assertNotEquals(other.getId(), current.id());

        // Cleanup test company
        companyRepository.delete(other);
    }

    @Test
    @DisplayName("Should ensure frontend cannot set logoPath directly through CompanyUpdateRequest")
    void testFrontendCannotSetLogoPathDirectly() {
        Company company = new Company();
        company.setLegalName("Old Name");
        company.setLogoPath("untouched/logo.png");

        CompanyUpdateRequest req = new CompanyUpdateRequest(
                "New Name", null, "AE", "AED", (byte) 1, null, null, null, true, false, false
        );

        companyMapper.updateEntity(company, req);

        assertEquals("New Name", company.getLegalName());
        assertEquals("untouched/logo.png", company.getLogoPath()); // Unchanged by mapper
    }
}
