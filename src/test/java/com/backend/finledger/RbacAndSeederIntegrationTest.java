package com.backend.finledger;

import com.backend.finledger.audit.controller.AuditLogController;
import com.backend.finledger.banking.repository.BankAccountRepository;
import com.backend.finledger.common.BusinessException;
import com.backend.finledger.common.DocType;
import com.backend.finledger.common.ForbiddenException;
import com.backend.finledger.common.NumberSeriesController;
import com.backend.finledger.common.NumberSeriesRepository;
import com.backend.finledger.company.entity.Company;
import com.backend.finledger.company.repository.CompanyRepository;
import com.backend.finledger.crm.dto.LeadDto;
import com.backend.finledger.crm.dto.LeadRequest;
import com.backend.finledger.crm.dto.OpportunityRequest;
import com.backend.finledger.crm.entity.LeadSource;
import com.backend.finledger.crm.repository.CrmPipelineStageRepository;
import com.backend.finledger.crm.service.LeadConversionService;
import com.backend.finledger.crm.service.LeadService;
import com.backend.finledger.crm.service.OpportunityService;
import org.springframework.data.domain.PageRequest;
import com.backend.finledger.expense.repository.ExpenseCategoryRepository;
import com.backend.finledger.expense.service.ExpenseService;
import com.backend.finledger.inventory.repository.WarehouseRepository;
import com.backend.finledger.ledger.entity.FiscalPeriod;
import com.backend.finledger.ledger.entity.FiscalPeriodStatus;
import com.backend.finledger.ledger.entity.FiscalYear;
import com.backend.finledger.ledger.repository.FiscalPeriodRepository;
import com.backend.finledger.ledger.repository.FiscalYearRepository;
import com.backend.finledger.security.UserPrincipal;
import com.backend.finledger.security.dto.AuthResponse;
import com.backend.finledger.security.dto.ChangePasswordRequest;
import com.backend.finledger.security.dto.LoginRequest;
import com.backend.finledger.security.service.AuthService;
import com.backend.finledger.tax.entity.TaxRate;
import com.backend.finledger.tax.repository.TaxRateRepository;
import com.backend.finledger.user.entity.User;
import com.backend.finledger.user.entity.UserStatus;
import com.backend.finledger.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("RBAC Enforcement, Password Change & DataSeeder Completeness Integration Tests")
class RbacAndSeederIntegrationTest extends BaseIntegrationTest {

    @Autowired private CompanyRepository companyRepository;
    @Autowired private FiscalYearRepository fiscalYearRepository;
    @Autowired private FiscalPeriodRepository fiscalPeriodRepository;
    @Autowired private NumberSeriesRepository numberSeriesRepository;
    @Autowired private WarehouseRepository warehouseRepository;
    @Autowired private BankAccountRepository bankAccountRepository;
    @Autowired private CrmPipelineStageRepository stageRepository;
    @Autowired private ExpenseCategoryRepository expenseCategoryRepository;
    @Autowired private TaxRateRepository taxRateRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private AuthService authService;
    @Autowired private OpportunityService opportunityService;
    @Autowired private LeadService leadService;
    @Autowired private LeadConversionService leadConversionService;
    @Autowired private ExpenseService expenseService;
    @Autowired private AuditLogController auditLogController;
    @Autowired private NumberSeriesController numberSeriesController;

    @Test
    @DisplayName("1. DataSeeder Operational Completeness: Fiscal periods, number series, warehouse, bank account, stages & categories exist")
    void testOperationalSeedsExistOnBoot() {
        Company bsb = companyRepository.findByCode("BSB").orElseThrow();
        Long cid = bsb.getId();

        // Fiscal year and 12 open periods
        String fyName = "FY" + LocalDate.now().getYear();
        FiscalYear fy = fiscalYearRepository.findByCompanyIdAndName(cid, fyName)
                .orElseThrow(() -> new AssertionError("Expected seeded Fiscal Year " + fyName));
        List<FiscalPeriod> periods = fiscalPeriodRepository.findByFiscalYearIdOrderByPeriodNoAsc(fy.getId());
        assertEquals(12, periods.size(), "Expected 12 fiscal periods");
        assertTrue(periods.stream().anyMatch(p -> p.getStatus() == FiscalPeriodStatus.OPEN));

        // All 12 document number series
        for (DocType docType : DocType.values()) {
            assertTrue(numberSeriesRepository.findByCompanyIdAndDocType(cid, docType).isPresent(),
                    "Missing seeded number series for " + docType);
        }

        // Warehouse, Bank Account, Stages, Categories
        assertFalse(warehouseRepository.findByCompanyId(cid).isEmpty(), "Expected seeded warehouse");
        assertFalse(bankAccountRepository.findByCompanyId(cid).isEmpty(), "Expected seeded bank account");
        assertFalse(stageRepository.findByCompanyIdOrderBySortOrderAsc(cid).isEmpty(), "Expected seeded CRM stages");
        assertFalse(expenseCategoryRepository.findByCompanyIdOrderByNameAsc(cid).isEmpty(), "Expected seeded expense categories");

        // VAT5 linked to sales & purchase tax GL accounts
        TaxRate vat5 = taxRateRepository.findByCompanyIdAndCode(cid, "VAT5").orElseThrow();
        assertNotNull(vat5.getSalesAccountId(), "VAT5 salesAccountId should be seeded");
        assertNotNull(vat5.getPurchaseAccountId(), "VAT5 purchaseAccountId should be seeded");
    }

    @Test
    @Transactional
    @DisplayName("2. Password Change Lifecycle: Rejects wrong current password, updates hash and permits login with new password")
    void testChangePassword() {
        Long companyId = companyRepository.findByCode("BSB").orElseThrow().getId();

        User u = new User();
        u.setCompanyId(companyId);
        u.setEmail("pwchange@finledger.com");
        u.setPasswordHash(passwordEncoder.encode("InitialPass1!"));
        u.setFullName("Password Test User");
        u.setStatus(UserStatus.ACTIVE);
        u.setFailedAttempts((byte) 0);
        User saved = userRepository.save(u);

        UserPrincipal principal = new UserPrincipal(saved.getId(), companyId, saved.getEmail(), "", saved.getFullName(), true, Set.of());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );

        // Wrong current password rejected
        BusinessException ex = assertThrows(BusinessException.class, () ->
                authService.changePassword(new ChangePasswordRequest("WrongPass", "NewSecurePass2!")));
        assertEquals("INVALID_PASSWORD", ex.getCode());

        // Correct password updates hash
        authService.changePassword(new ChangePasswordRequest("InitialPass1!", "NewSecurePass2!"));

        // Login with new password succeeds
        AuthResponse loginRes = authService.login(new LoginRequest("pwchange@finledger.com", "NewSecurePass2!"));
        assertNotNull(loginRes.accessToken());
    }

    @Test
    @DisplayName("3. @PreAuthorize RBAC Enforcement: Unprivileged user blocked from CRM conversion, Opportunity creation & Expense approval")
    void testGranularRbacBlocking() {
        Long companyId = companyRepository.findByCode("BSB").orElseThrow().getId();

        // Simulate EMPLOYEE holding only EXPENSE_VIEW & EXPENSE_CREATE
        UserPrincipal employee = new UserPrincipal(
                99L, companyId, "employee@finledger.com", "", "Regular Employee", true,
                Set.of("EXPENSE_VIEW", "EXPENSE_CREATE")
        );
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(employee, null, employee.getAuthorities())
        );

        assertThrows(AccessDeniedException.class, () ->
                expenseService.approveExpense(1L, "Unauthorized approval"));

        assertThrows(AccessDeniedException.class, () ->
                leadConversionService.convertLead(1L, null));

        assertThrows(AccessDeniedException.class, () ->
                opportunityService.createOpportunity(new OpportunityRequest(
                        "Deal", null, null, null, 1L, BigDecimal.TEN, "AED", (byte) 10, LocalDate.now(), null
                )));
    }

    @Test
    @Transactional
    @DisplayName("4. Spec Section 5.5 Row-Level Scoping & Admin Endpoints: Sales reps see only own leads unless CRM_VIEW_ALL")
    void testRowLevelOwnerScopingAndAdminControllers() {
        Long companyId = companyRepository.findByCode("BSB").orElseThrow().getId();

        User rep1Entity = userRepository.save(createTestUser(companyId, "rep1@bsb.ae", "Sales Rep 1"));
        User rep2Entity = userRepository.save(createTestUser(companyId, "rep2@bsb.ae", "Sales Rep 2"));
        User mgrEntity = userRepository.save(createTestUser(companyId, "mgr@bsb.ae", "Sales Manager"));

        // Sales Rep 1 creates a lead
        UserPrincipal rep1 = new UserPrincipal(rep1Entity.getId(), companyId, rep1Entity.getEmail(), "", rep1Entity.getFullName(), true,
                Set.of("LEAD_VIEW", "LEAD_CREATE"));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(rep1, null, rep1.getAuthorities()));

        LeadDto rep1Lead = leadService.createLead(new LeadRequest(
                "Rep1 Client Co", "Alice", "alice@client.ae", null, LeadSource.WEBSITE,
                "Tech", "Dubai", "AE", rep1Entity.getId(), "Owned by Rep 1"));

        // Sales Rep 2 (lacks CRM_VIEW_ALL) cannot read Rep 1's lead by ID or see it in getLeads()
        UserPrincipal rep2 = new UserPrincipal(rep2Entity.getId(), companyId, rep2Entity.getEmail(), "", rep2Entity.getFullName(), true,
                Set.of("LEAD_VIEW", "LEAD_CREATE"));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(rep2, null, rep2.getAuthorities()));

        assertThrows(ForbiddenException.class, () -> leadService.getLeadById(rep1Lead.id()));
        var rep2List = leadService.getLeads(null, null, null, null, PageRequest.of(0, 50));
        assertTrue(rep2List.content().stream().noneMatch(l -> l.id().equals(rep1Lead.id())));

        // Sales Manager (with CRM_VIEW_ALL + SETTINGS_EDIT) sees Rep 1's lead, Number Series, and Audit Logs
        UserPrincipal manager = new UserPrincipal(mgrEntity.getId(), companyId, mgrEntity.getEmail(), "", mgrEntity.getFullName(), true,
                Set.of("LEAD_VIEW", "CRM_VIEW_ALL", "SETTINGS_EDIT", "COMPANY_VIEW"));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(manager, null, manager.getAuthorities()));

        assertEquals(rep1Lead.id(), leadService.getLeadById(rep1Lead.id()).id());
        assertFalse(numberSeriesController.listSeries().getBody().isEmpty());
        assertNotNull(auditLogController.searchLogs(null, null, null, null, PageRequest.of(0, 10)).getBody());
    }

    private User createTestUser(Long companyId, String email, String fullName) {
        return userRepository.findByEmail(email).orElseGet(() -> {
            User u = new User();
            u.setCompanyId(companyId);
            u.setEmail(email);
            u.setPasswordHash(passwordEncoder.encode("Secret123!"));
            u.setFullName(fullName);
            u.setStatus(UserStatus.ACTIVE);
            u.setFailedAttempts((byte) 0);
            return u;
        });
    }
}

