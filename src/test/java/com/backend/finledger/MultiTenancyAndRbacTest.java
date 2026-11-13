package com.backend.finledger;

import com.backend.finledger.banking.dto.BankAccountRequest;
import com.backend.finledger.banking.dto.BankReconciliationRequest;
import com.backend.finledger.banking.service.BankAccountService;
import com.backend.finledger.banking.service.BankReconciliationService;
import com.backend.finledger.common.BusinessException;
import com.backend.finledger.company.entity.Company;
import com.backend.finledger.company.repository.CompanyRepository;
import com.backend.finledger.ledger.dto.AccountRequest;
import com.backend.finledger.ledger.dto.CreateJournalEntryRequest;
import com.backend.finledger.ledger.entity.Account;
import com.backend.finledger.ledger.entity.AccountType;
import com.backend.finledger.ledger.entity.NormalBalance;
import com.backend.finledger.ledger.repository.AccountRepository;
import com.backend.finledger.ledger.service.AccountService;
import com.backend.finledger.ledger.service.FiscalYearPeriodService;
import com.backend.finledger.ledger.service.PostingService;
import com.backend.finledger.report.service.ReportService;
import com.backend.finledger.security.UserPrincipal;
import com.backend.finledger.tax.dto.TaxRateDto;
import com.backend.finledger.tax.dto.TaxRateRequest;
import com.backend.finledger.tax.entity.TaxRate;
import com.backend.finledger.tax.entity.TaxType;
import com.backend.finledger.tax.repository.TaxRateRepository;
import com.backend.finledger.tax.service.TaxRateService;
import com.backend.finledger.user.dto.CreateUserRequest;
import com.backend.finledger.user.entity.User;
import com.backend.finledger.user.repository.UserRepository;
import com.backend.finledger.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class MultiTenancyAndRbacTest {

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TaxRateRepository taxRateRepository;

    @Autowired
    private TaxRateService taxRateService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private AccountService accountService;

    @Autowired
    private UserService userService;

    @Autowired
    private PostingService postingService;

    @Autowired
    private FiscalYearPeriodService fiscalYearPeriodService;

    @Autowired
    private BankAccountService bankAccountService;

    @Autowired
    private BankReconciliationService bankReconciliationService;

    @Autowired
    private ReportService reportService;

    private Long bsbCompanyId;
    private Long acmeCompanyId;

    @BeforeEach
    void setupCompanies() {
        Company bsb = companyRepository.findByCode("BSB").orElseThrow();
        bsbCompanyId = bsb.getId();

        Company acme = companyRepository.findByCode("ACME").orElseGet(() -> {
            Company c = new Company();
            c.setCode("ACME");
            c.setLegalName("ACME Corporation FZE");
            c.setCountryCode("AE");
            c.setBaseCurrency("AED");
            c.setFiscalYearStartMonth((byte) 1);
            return companyRepository.save(c);
        });
        acmeCompanyId = acme.getId();
    }

    private void authenticateAs(Long userId, Long companyId, String email, Set<String> perms) {
        UserPrincipal principal = new UserPrincipal(
                userId,
                companyId,
                email,
                "",
                "Test User",
                true,
                perms
        );
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    @Transactional
    @DisplayName("Tenant isolation: Company B should never see or access Company A's tax rates")
    void testCrossCompanyDataIsolation_TaxRates() {
        // Authenticate as BSB Admin
        authenticateAs(1L, bsbCompanyId, "admin@finledger.com", Set.of("TAX_RATE_VIEW", "TAX_RATE_EDIT", "SETTINGS_EDIT"));
        TaxRate bsbRate = taxRateRepository.findByCompanyIdAndCode(bsbCompanyId, "VAT5").orElseThrow();

        // Switch to ACME Admin
        authenticateAs(2L, acmeCompanyId, "acme_admin@finledger.com", Set.of("TAX_RATE_VIEW", "TAX_RATE_EDIT", "SETTINGS_EDIT"));

        // ACME list should not include BSB tax rates
        List<TaxRateDto> acmeRates = taxRateService.getTaxRates();
        assertTrue(acmeRates.stream().noneMatch(tr -> tr.id().equals(bsbRate.getId())));

        // ACME direct lookup by BSB rate id must be denied with TAX_RATE_NOT_FOUND
        BusinessException ex = assertThrows(BusinessException.class, () ->
                taxRateService.getTaxRateById(bsbRate.getId()));
        assertEquals("TAX_RATE_NOT_FOUND", ex.getCode());
    }

    @Test
    @Transactional
    @DisplayName("Tenant isolation: Company B should never access Company A's users")
    void testCrossCompanyDataIsolation_Users() {
        User bsbUser = userRepository.findByEmail("admin@finledger.com").orElseThrow();

        // Switch context to ACME company
        authenticateAs(99L, acmeCompanyId, "acme_admin@finledger.com", Set.of("USER_VIEW", "USER_CREATE", "SETTINGS_EDIT"));

        // Direct lookup of BSB user from ACME context must fail with USER_NOT_FOUND
        BusinessException ex = assertThrows(BusinessException.class, () ->
                userService.getUserById(bsbUser.getId()));
        assertEquals("USER_NOT_FOUND", ex.getCode());
    }

    @Test
    @Transactional
    @DisplayName("Tenant isolation: Company B should never access Company A's accounts")
    void testCrossCompanyDataIsolation_Accounts() {
        Account bsbAccount = accountRepository.findByCompanyIdAndCode(bsbCompanyId, "1000").orElseThrow();

        // Switch context to ACME company
        authenticateAs(99L, acmeCompanyId, "acme_admin@finledger.com", Set.of("ACCOUNT_VIEW", "ACCOUNT_EDIT", "SETTINGS_EDIT"));

        // Direct lookup of BSB account from ACME context must fail with ACCOUNT_NOT_FOUND
        BusinessException ex = assertThrows(BusinessException.class, () ->
                accountService.getAccountById(bsbAccount.getId()));
        assertEquals("ACCOUNT_NOT_FOUND", ex.getCode());
    }

    @Test
    @Transactional
    @DisplayName("RBAC: VIEWER role cannot perform mutating operations (@PreAuthorize enforcement)")
    void testRbacAuthorization_ViewerDeniedWrite() {
        // Authenticate with read-only permissions
        Set<String> viewerPerms = Set.of(
                "COMPANY_VIEW", "USER_VIEW", "ROLE_VIEW", "ACCOUNT_VIEW", "TAX_RATE_VIEW", "CURRENCY_VIEW"
        );
        authenticateAs(10L, bsbCompanyId, "viewer@finledger.com", viewerPerms);

        // Attempt to create a user should fail with AccessDeniedException
        CreateUserRequest userReq = new CreateUserRequest("test_unauth@finledger.com", "pass123", "Unauthorized User", Set.of());
        assertThrows(AccessDeniedException.class, () -> userService.createUser(userReq));

        // Attempt to create a tax rate should fail with AccessDeniedException
        TaxRateRequest taxReq = new TaxRateRequest("VAT99", "Fake Tax", new BigDecimal("9.0000"), TaxType.STANDARD, null, null, true);
        assertThrows(AccessDeniedException.class, () -> taxRateService.createTaxRate(taxReq));

        // Attempt to create an account should fail with AccessDeniedException
        AccountRequest accReq = new AccountRequest("9999", "Fake Account", AccountType.EXPENSE, null, null, false, NormalBalance.DEBIT, "AED", BigDecimal.ZERO, true);
        assertThrows(AccessDeniedException.class, () -> accountService.createAccount(accReq));

        // Attempt to close a fiscal period or create bank reconciliation should fail
        assertThrows(AccessDeniedException.class, () -> fiscalYearPeriodService.closePeriod(1L));
        assertThrows(AccessDeniedException.class, () -> bankAccountService.createBankAccount(
                new BankAccountRequest("Unauth", "Bank", "000", null, null, "AED", 1L, null, true)));
        assertThrows(AccessDeniedException.class, () -> bankReconciliationService.startReconciliation(
                new BankReconciliationRequest(1L, LocalDate.now(), BigDecimal.ZERO)));
    }

    @Test
    @Transactional
    @DisplayName("RBAC: EMPLOYEE role cannot bypass authorization into Reports, Journals, COA or Bank Reconciliation")
    void testRbacAuthorization_EmployeeDeniedPrivilegedReadsAndWrites() {
        authenticateAs(11L, bsbCompanyId, "emp@finledger.com", Set.of("EXPENSE_VIEW", "EXPENSE_CREATE"));

        assertThrows(AccessDeniedException.class, () -> reportService.getProfitLossReport(null, null, null, null));
        assertThrows(AccessDeniedException.class, () -> postingService.getJournalEntries(null, null, null, null, PageRequest.of(0, 10)));
        assertThrows(AccessDeniedException.class, () -> accountService.getAccounts(null, null, null, PageRequest.of(0, 10)));
        assertThrows(AccessDeniedException.class, () -> bankReconciliationService.autoMatch(1L));
        assertThrows(AccessDeniedException.class, () -> postingService.createJournalEntry(
                new CreateJournalEntryRequest(LocalDate.now(), "REF", "Unauth", null, null, "AED", BigDecimal.ONE, List.of(), false)));
    }

    @Test
    @Transactional
    @DisplayName("RBAC: User with proper authorities is permitted to perform writes")
    void testRbacAuthorization_AuthorizedUserAllowedWrite() {
        // Authenticate with write permissions
        Set<String> adminPerms = Set.of("TAX_RATE_VIEW", "TAX_RATE_EDIT", "SETTINGS_EDIT");
        authenticateAs(1L, bsbCompanyId, "admin@finledger.com", adminPerms);

        TaxRateRequest taxReq = new TaxRateRequest("VAT7", "UAE Special VAT 7%", new BigDecimal("7.0000"), TaxType.STANDARD, null, null, true);
        TaxRateDto created = taxRateService.createTaxRate(taxReq);
        assertNotNull(created);
        assertEquals("VAT7", created.code());
    }
}

