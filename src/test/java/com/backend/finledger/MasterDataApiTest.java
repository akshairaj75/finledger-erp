package com.backend.finledger;

import com.backend.finledger.common.BusinessException;
import com.backend.finledger.company.CompanyService;
import com.backend.finledger.company.dto.CompanyDto;
import com.backend.finledger.currency.CurrencyService;
import com.backend.finledger.currency.dto.CurrencyDto;
import com.backend.finledger.currency.dto.ExchangeRateDto;
import com.backend.finledger.currency.dto.ExchangeRateRequest;
import com.backend.finledger.ledger.Account;
import com.backend.finledger.ledger.AccountRepository;
import com.backend.finledger.ledger.AccountService;
import com.backend.finledger.ledger.dto.AccountNodeDto;
import com.backend.finledger.security.UserPrincipal;
import com.backend.finledger.tax.TaxRateService;
import com.backend.finledger.tax.dto.TaxRateDto;
import com.backend.finledger.user.UserService;
import com.backend.finledger.user.dto.RoleDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class MasterDataApiTest {

    @Autowired
    private CompanyService companyService;

    @Autowired
    private com.backend.finledger.company.CompanyRepository companyRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private CurrencyService currencyService;

    @Autowired
    private TaxRateService taxRateService;

    @Autowired
    private AccountService accountService;

    @Autowired
    private AccountRepository accountRepository;

    private Long companyId;

    @BeforeEach
    void setUpContext() {
        // Find default seeded company ID
        companyId = companyRepository.findByCode("BSB").orElseThrow().getId();
        UserPrincipal principal = new UserPrincipal(
                1L,
                companyId,
                "admin@finledger.com",
                "",
                "System Administrator",
                true,
                Set.of("SETTINGS_EDIT", "COMPANY_VIEW", "COMPANY_EDIT", "USER_VIEW", "USER_CREATE",
                       "ROLE_VIEW", "ACCOUNT_VIEW", "ACCOUNT_CREATE", "ACCOUNT_EDIT", "ACCOUNT_DELETE",
                       "TAX_RATE_VIEW", "TAX_RATE_EDIT", "CURRENCY_VIEW", "CURRENCY_EDIT")
        );

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities()
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    @Transactional
    @DisplayName("Should retrieve current company profile scoped to JWT company_id")
    void testCompanyCurrent() {
        CompanyDto company = companyService.getCurrentCompany();
        assertNotNull(company);
        assertEquals("BSB", company.code());
        assertEquals("AED", company.baseCurrency());
    }

    @Test
    @Transactional
    @DisplayName("Should retrieve seeded roles and permissions")
    void testRolesAndPermissions() {
        List<RoleDto> roles = userService.getRoles();
        assertFalse(roles.isEmpty());
        assertTrue(roles.stream().anyMatch(r -> r.name().equals("ADMIN")));
        assertTrue(roles.stream().anyMatch(r -> r.name().equals("ACCOUNTANT")));
        assertTrue(roles.stream().anyMatch(r -> r.name().equals("SALES_REP")));
        assertTrue(roles.stream().anyMatch(r -> r.name().equals("SALES_MANAGER")));
        assertTrue(roles.stream().anyMatch(r -> r.name().equals("AR_CLERK")));
        assertTrue(roles.stream().anyMatch(r -> r.name().equals("AP_CLERK")));
        assertTrue(roles.stream().anyMatch(r -> r.name().equals("STOREKEEPER")));
        assertTrue(roles.stream().anyMatch(r -> r.name().equals("EMPLOYEE")));
        assertTrue(roles.stream().anyMatch(r -> r.name().equals("VIEWER")));
    }

    @Test
    @Transactional
    @DisplayName("Should support exchange rate with DECIMAL(18,6) precision")
    void testExchangeRates() {
        List<CurrencyDto> currencies = currencyService.getAllCurrencies();
        assertTrue(currencies.stream().anyMatch(c -> c.code().equals("AED")));
        assertTrue(currencies.stream().anyMatch(c -> c.code().equals("USD")));

        BigDecimal preciseRate = new BigDecimal("3.672500");
        ExchangeRateRequest req = new ExchangeRateRequest("USD", "AED", LocalDate.now(), preciseRate);
        ExchangeRateDto saved = currencyService.setExchangeRate(req);

        assertEquals(0, preciseRate.compareTo(saved.rate()));
    }

    @Test
    @Transactional
    @DisplayName("Should verify seeded UAE 5% and KSA 15% tax rates")
    void testTaxRates() {
        List<TaxRateDto> taxRates = taxRateService.getTaxRates();
        assertTrue(taxRates.stream().anyMatch(tr -> tr.code().equals("VAT5") && tr.rate().compareTo(new BigDecimal("5.0000")) == 0));
        assertTrue(taxRates.stream().anyMatch(tr -> tr.code().equals("VAT15") && tr.rate().compareTo(new BigDecimal("15.0000")) == 0));
    }

    @Test
    @Transactional
    @DisplayName("Should retrieve hierarchical Chart of Accounts tree and prevent deleting system accounts")
    void testChartOfAccountsTreeAndImmutability() {
        List<AccountNodeDto> tree = accountService.getAccountsTree();
        assertFalse(tree.isEmpty());
        // Verify root groups
        assertTrue(tree.stream().anyMatch(node -> node.getCode().equals("1000"))); // Assets
        assertTrue(tree.stream().anyMatch(node -> node.getCode().equals("2000"))); // Liabilities

        // Find system account (e.g. 1000 Assets or 1200 Accounts Receivable)
        Account arAccount = accountRepository.findByCompanyIdAndCode(companyId, "1200").orElseThrow();
        assertTrue(arAccount.getIsSystem());

        BusinessException ex = assertThrows(BusinessException.class, () ->
                accountService.deleteAccount(arAccount.getId()));
        assertEquals("SYSTEM_ACCOUNT_IMMUTABLE", ex.getCode());
    }
}

