package com.backend.finledger;

import com.backend.finledger.common.BusinessException;
import com.backend.finledger.company.entity.Company;
import com.backend.finledger.company.repository.CompanyRepository;
import com.backend.finledger.ledger.dto.CreateFiscalYearRequest;
import com.backend.finledger.ledger.dto.CreateJournalEntryRequest;
import com.backend.finledger.ledger.dto.FiscalYearDto;
import com.backend.finledger.ledger.dto.JournalEntryDto;
import com.backend.finledger.ledger.dto.JournalLineRequest;
import com.backend.finledger.ledger.entity.*;
import com.backend.finledger.ledger.repository.AccountRepository;
import com.backend.finledger.ledger.repository.FiscalPeriodRepository;
import com.backend.finledger.ledger.repository.FiscalYearRepository;
import com.backend.finledger.ledger.repository.JournalEntryRepository;
import com.backend.finledger.ledger.service.FiscalYearPeriodService;
import com.backend.finledger.ledger.service.PostingService;
import com.backend.finledger.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("PostingService Integration Tests (Double-Entry Balance, Closed Periods, Void Reversals)")
class PostingServiceIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private PostingService postingService;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private FiscalYearRepository fiscalYearRepository;

    @Autowired
    private FiscalPeriodRepository fiscalPeriodRepository;

    @Autowired
    private FiscalYearPeriodService fiscalYearPeriodService;

    @Autowired
    private JournalEntryRepository journalEntryRepository;

    private Long companyId;
    private Long debitAccountId;
    private Long creditAccountId;
    private FiscalPeriod openPeriod;
    private FiscalPeriod closedPeriod;

    @BeforeEach
    void setUp() {
        Company company = companyRepository.findByCode("BSB").orElseGet(() -> {
            Company c = new Company();
            c.setCode("BSB");
            c.setLegalName("Blue Sky Business LLC");
            c.setCountryCode("AE");
            c.setBaseCurrency("AED");
            c.setFiscalYearStartMonth((byte) 1);
            return companyRepository.save(c);
        });
        companyId = company.getId();

        UserPrincipal principal = new UserPrincipal(
                1L,
                companyId,
                "admin@finledger.com",
                "Password123!",
                "System Admin",
                true,
                Set.of("JOURNAL_VIEW", "JOURNAL_CREATE", "JOURNAL_POST", "JOURNAL_VOID", "LEDGER_VIEW")
        );
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );

        Account debitAcc = accountRepository.findByCompanyIdAndCode(companyId, "1110").orElseGet(() -> {
            Account acc = new Account(companyId, "1110", "Cash on Hand", AccountType.ASSET, "CASH", null, false, NormalBalance.DEBIT);
            acc.setIsActive(true);
            return accountRepository.save(acc);
        });
        debitAccountId = debitAcc.getId();

        Account creditAcc = accountRepository.findByCompanyIdAndCode(companyId, "4100").orElseGet(() -> {
            Account acc = new Account(companyId, "4100", "Sales Revenue", AccountType.INCOME, "REVENUE", null, false, NormalBalance.CREDIT);
            acc.setIsActive(true);
            return accountRepository.save(acc);
        });
        creditAccountId = creditAcc.getId();

        FiscalYear fy = fiscalYearRepository.findByCompanyIdAndName(companyId, "FY2026").orElseGet(() -> {
            CreateFiscalYearRequest fyReq = new CreateFiscalYearRequest(
                    "FY2026",
                    LocalDate.of(2026, 1, 1),
                    LocalDate.of(2026, 12, 31),
                    true
            );
            FiscalYearDto created = fiscalYearPeriodService.createFiscalYear(fyReq);
            return fiscalYearRepository.findById(created.id()).orElseThrow();
        });

        List<FiscalPeriod> periods = fiscalPeriodRepository.findByFiscalYearIdOrderByPeriodNoAsc(fy.getId());
        openPeriod = periods.get(0);
        if (openPeriod.getStatus() != FiscalPeriodStatus.OPEN) {
            openPeriod.setStatus(FiscalPeriodStatus.OPEN);
            openPeriod = fiscalPeriodRepository.save(openPeriod);
        }

        closedPeriod = periods.get(1);
        closedPeriod.setStatus(FiscalPeriodStatus.CLOSED);
        closedPeriod = fiscalPeriodRepository.save(closedPeriod);
    }

    @Test
    @Transactional
    @DisplayName("1. Double-Entry Balance Check: Entry where Debit != Credit must be rejected with JOURNAL_UNBALANCED")
    void testUnbalancedJournalEntryMustBeRejected() {
        CreateJournalEntryRequest request = new CreateJournalEntryRequest(
                openPeriod.getStartDate(),
                "UNBAL-001",
                "Unbalanced entry test",
                JournalSourceType.MANUAL,
                null,
                "AED",
                BigDecimal.ONE,
                List.of(
                        new JournalLineRequest(debitAccountId, null, "Debit cash", new BigDecimal("1000.00"), BigDecimal.ZERO, null, null),
                        new JournalLineRequest(creditAccountId, null, "Credit revenue", BigDecimal.ZERO, new BigDecimal("950.00"), null, null)
                ),
                true // attempt auto-post
        );

        BusinessException ex = assertThrows(BusinessException.class, () -> postingService.createJournalEntry(request));
        assertEquals("JOURNAL_UNBALANCED", ex.getCode(), "Expected JOURNAL_UNBALANCED exception code");
        assertTrue(ex.getMessage().toLowerCase().contains("total debit"), "Exception message should describe imbalance");
    }

    @Test
    @Transactional
    @DisplayName("2. Closed Period Check: Posting into a CLOSED fiscal period must be rejected with PERIOD_CLOSED")
    void testPostingIntoClosedPeriodMustBeRejected() {
        CreateJournalEntryRequest request = new CreateJournalEntryRequest(
                closedPeriod.getStartDate(),
                "CLOSED-001",
                "Closed period test",
                JournalSourceType.MANUAL,
                null,
                "AED",
                BigDecimal.ONE,
                List.of(
                        new JournalLineRequest(debitAccountId, null, "Debit cash", new BigDecimal("500.00"), BigDecimal.ZERO, null, null),
                        new JournalLineRequest(creditAccountId, null, "Credit revenue", BigDecimal.ZERO, new BigDecimal("500.00"), null, null)
                ),
                true // attempt auto-post
        );

        BusinessException ex = assertThrows(BusinessException.class, () -> postingService.createJournalEntry(request));
        assertEquals("PERIOD_CLOSED", ex.getCode(), "Expected PERIOD_CLOSED exception code");
    }

    @Test
    @Transactional
    @DisplayName("3. Void Reversal Entry: Reversing posted journal creates reversing entry with swapped debits/credits, marks original as VOID")
    void testVoidReversalEntrySwapsDebitsAndCredits() {
        CreateJournalEntryRequest request = new CreateJournalEntryRequest(
                openPeriod.getStartDate(),
                "REV-ORIG-001",
                "Balanced original entry",
                JournalSourceType.MANUAL,
                null,
                "AED",
                BigDecimal.ONE,
                List.of(
                        new JournalLineRequest(debitAccountId, null, "Original Debit", new BigDecimal("750.00"), BigDecimal.ZERO, null, null),
                        new JournalLineRequest(creditAccountId, null, "Original Credit", BigDecimal.ZERO, new BigDecimal("750.00"), null, null)
                ),
                true
        );

        JournalEntryDto originalPosted = postingService.createJournalEntry(request);
        assertEquals(JournalStatus.POSTED, originalPosted.status());
        Long originalId = originalPosted.id();

        JournalEntryDto reversal = postingService.reverseJournalEntry(originalId, "Audit correction: incorrect allocation");

        assertNotNull(reversal);
        assertNotNull(reversal.id());
        assertNotEquals(originalId, reversal.id());
        assertEquals(JournalStatus.POSTED, reversal.status(), "Reversal entry itself must be POSTED");
        assertEquals(originalId, reversal.reversalOfId(), "Reversal entry must point to original id");
        assertEquals(new BigDecimal("750.00"), reversal.totalDebit());
        assertEquals(new BigDecimal("750.00"), reversal.totalCredit());

        JournalEntry reloadedOriginal = journalEntryRepository.findById(originalId).orElseThrow();
        assertEquals(JournalStatus.VOID, reloadedOriginal.getStatus(), "Original entry must be marked as VOID");

        BusinessException dupEx = assertThrows(BusinessException.class,
                () -> postingService.reverseJournalEntry(originalId, "Second attempt"));
        assertTrue(dupEx.getCode().equals("CANNOT_REVERSE_UNPOSTED") || dupEx.getCode().equals("DUPLICATE_REVERSAL"));
    }
}
