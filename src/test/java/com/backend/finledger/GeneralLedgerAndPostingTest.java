package com.backend.finledger;

import com.backend.finledger.common.BusinessException;
import com.backend.finledger.company.entity.Company;
import com.backend.finledger.company.repository.CompanyRepository;
import com.backend.finledger.ledger.dto.*;
import com.backend.finledger.ledger.entity.*;
import com.backend.finledger.ledger.repository.*;
import com.backend.finledger.ledger.service.*;
import com.backend.finledger.report.service.ReportService;
import com.backend.finledger.security.CurrentUser;
import com.backend.finledger.security.UserPrincipal;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class GeneralLedgerAndPostingTest {

    @Autowired
    private PostingService postingService;

    @Autowired
    private FiscalYearPeriodService fiscalYearPeriodService;

    @Autowired
    private CostCenterService costCenterService;

    @Autowired
    private ReportService reportService;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private CostCenterRepository costCenterRepository;

    @Autowired
    private FiscalYearRepository fiscalYearRepository;

    @Autowired
    private FiscalPeriodRepository fiscalPeriodRepository;

    @Autowired
    private JournalEntryRepository journalEntryRepository;

    private Long bsbCompanyId;
    private Long acmeCompanyId;

    private Long cashAccountId;
    private Long salesAccountId;
    private Long groupAccountId;
    private Long inactiveAccountId;
    private Long acmeAccountId;

    private Long bsbCostCenterId;
    private Long acmeCostCenterId;

    private FiscalPeriod openPeriod;
    private FiscalPeriod closedPeriod;
    private FiscalPeriod lockedPeriod;

    @BeforeEach
    void setUp() {
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

        authenticateAs(1L, bsbCompanyId, "admin@finledger.com", Set.of(
                "JOURNAL_VIEW", "JOURNAL_CREATE", "JOURNAL_POST", "JOURNAL_VOID",
                "REPORT_VIEW", "LEDGER_VIEW", "SETTINGS_EDIT", "ACCOUNT_VIEW", "ACCOUNT_EDIT"
        ));

        // 1. Ensure Fiscal Year & Periods exist for BSB for 2026
        FiscalYear fy = fiscalYearRepository.findByCompanyIdAndName(bsbCompanyId, "FY2026").orElseGet(() -> {
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
        openPeriod = periods.get(0); // Period 1 (Jan 2026) - OPEN

        // Period 2 (Feb 2026) - set to CLOSED
        closedPeriod = periods.get(1);
        closedPeriod.setStatus(FiscalPeriodStatus.CLOSED);
        fiscalPeriodRepository.save(closedPeriod);

        // Period 3 (Mar 2026) - set to LOCKED
        lockedPeriod = periods.get(2);
        lockedPeriod.setStatus(FiscalPeriodStatus.LOCKED);
        fiscalPeriodRepository.save(lockedPeriod);

        // 2. Fetch or create accounts
        Account cash = accountRepository.findByCompanyIdAndCode(bsbCompanyId, "1110").orElseThrow();
        cashAccountId = cash.getId();

        Account sales = accountRepository.findByCompanyIdAndCode(bsbCompanyId, "4100").orElseThrow();
        salesAccountId = sales.getId();

        Account groupAcc = accountRepository.findByCompanyIdAndCode(bsbCompanyId, "1000").orElseThrow();
        groupAccountId = groupAcc.getId();

        Account inactive = accountRepository.findByCompanyIdAndCode(bsbCompanyId, "9998").orElseGet(() -> {
            Account acc = new Account(bsbCompanyId, "9998", "Inactive Account", AccountType.EXPENSE, null, null, false, NormalBalance.DEBIT);
            acc.setIsActive(false);
            return accountRepository.save(acc);
        });
        inactiveAccountId = inactive.getId();

        Account acmeAcc = accountRepository.findByCompanyIdAndCode(acmeCompanyId, "1110").orElseGet(() -> {
            Account acc = new Account(acmeCompanyId, "1110", "ACME Cash", AccountType.ASSET, "CASH", null, false, NormalBalance.DEBIT);
            acc.setIsActive(true);
            return accountRepository.save(acc);
        });
        acmeAccountId = acmeAcc.getId();

        // 3. Cost centers
        CostCenter bsbCc = costCenterRepository.findByCompanyIdAndCode(bsbCompanyId, "CC-DXB").orElseGet(() -> {
            CostCenter cc = new CostCenter(bsbCompanyId, "CC-DXB", "Dubai Branch", null);
            return costCenterRepository.save(cc);
        });
        bsbCostCenterId = bsbCc.getId();

        CostCenter acmeCc = costCenterRepository.findByCompanyIdAndCode(acmeCompanyId, "CC-SHJ").orElseGet(() -> {
            CostCenter cc = new CostCenter(acmeCompanyId, "CC-SHJ", "Sharjah Branch", null);
            return costCenterRepository.save(cc);
        });
        acmeCostCenterId = acmeCc.getId();
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
    @DisplayName("1. Validation Rule: Journal with fewer than two lines must be rejected")
    void testFewerThanTwoLinesRejected() {
        CreateJournalEntryRequest req = new CreateJournalEntryRequest(
                openPeriod.getStartDate(),
                "REF-001",
                "Single line test",
                JournalSourceType.MANUAL,
                null,
                "AED",
                BigDecimal.ONE,
                List.of(new JournalLineRequest(cashAccountId, null, "Single line", new BigDecimal("100.00"), BigDecimal.ZERO, null, null)),
                true
        );

        BusinessException ex = assertThrows(BusinessException.class, () -> postingService.createJournalEntry(req));
        assertEquals("JOURNAL_MIN_LINES", ex.getCode());
    }

    @Test
    @Transactional
    @DisplayName("2. Validation Rule: Unbalanced journal (debit != credit) must be rejected")
    void testUnbalancedJournalRejected() {
        CreateJournalEntryRequest req = new CreateJournalEntryRequest(
                openPeriod.getStartDate(),
                "REF-002",
                "Unbalanced test",
                JournalSourceType.MANUAL,
                null,
                "AED",
                BigDecimal.ONE,
                List.of(
                        new JournalLineRequest(cashAccountId, null, "Debit 100", new BigDecimal("100.00"), BigDecimal.ZERO, null, null),
                        new JournalLineRequest(salesAccountId, null, "Credit 90", BigDecimal.ZERO, new BigDecimal("90.00"), null, null)
                ),
                true
        );

        BusinessException ex = assertThrows(BusinessException.class, () -> postingService.createJournalEntry(req));
        assertEquals("JOURNAL_UNBALANCED", ex.getCode());
    }

    @Test
    @Transactional
    @DisplayName("3. Validation Rule: Line with both debit and credit must be rejected")
    void testLineWithDebitAndCreditRejected() {
        CreateJournalEntryRequest req = new CreateJournalEntryRequest(
                openPeriod.getStartDate(),
                "REF-003",
                "Both amounts test",
                JournalSourceType.MANUAL,
                null,
                "AED",
                BigDecimal.ONE,
                List.of(
                        new JournalLineRequest(cashAccountId, null, "Both amounts", new BigDecimal("100.00"), new BigDecimal("50.00"), null, null),
                        new JournalLineRequest(salesAccountId, null, "Credit 50", BigDecimal.ZERO, new BigDecimal("50.00"), null, null)
                ),
                true
        );

        BusinessException ex = assertThrows(BusinessException.class, () -> postingService.createJournalEntry(req));
        assertEquals("INVALID_LINE_AMOUNTS", ex.getCode());
    }

    @Test
    @Transactional
    @DisplayName("4. Validation Rule: Negative debit or credit must be rejected")
    void testNegativeAmountRejected() {
        CreateJournalEntryRequest req = new CreateJournalEntryRequest(
                openPeriod.getStartDate(),
                "REF-004",
                "Negative amount test",
                JournalSourceType.MANUAL,
                null,
                "AED",
                BigDecimal.ONE,
                List.of(
                        new JournalLineRequest(cashAccountId, null, "Debit -100", new BigDecimal("-100.00"), BigDecimal.ZERO, null, null),
                        new JournalLineRequest(salesAccountId, null, "Credit -100", BigDecimal.ZERO, new BigDecimal("-100.00"), null, null)
                ),
                true
        );

        BusinessException ex = assertThrows(BusinessException.class, () -> postingService.createJournalEntry(req));
        assertEquals("INVALID_LINE_AMOUNTS", ex.getCode());
    }

    @Test
    @Transactional
    @DisplayName("5. Validation Rule: Group account cannot accept postings")
    void testGroupAccountPostingRejected() {
        CreateJournalEntryRequest req = new CreateJournalEntryRequest(
                openPeriod.getStartDate(),
                "REF-005",
                "Group account test",
                JournalSourceType.MANUAL,
                null,
                "AED",
                BigDecimal.ONE,
                List.of(
                        new JournalLineRequest(groupAccountId, null, "Group account debit", new BigDecimal("100.00"), BigDecimal.ZERO, null, null),
                        new JournalLineRequest(salesAccountId, null, "Credit sales", BigDecimal.ZERO, new BigDecimal("100.00"), null, null)
                ),
                true
        );

        BusinessException ex = assertThrows(BusinessException.class, () -> postingService.createJournalEntry(req));
        assertEquals("GROUP_ACCOUNT_CANNOT_POST", ex.getCode());
    }

    @Test
    @Transactional
    @DisplayName("6. Validation Rule: Inactive account cannot accept postings")
    void testInactiveAccountPostingRejected() {
        CreateJournalEntryRequest req = new CreateJournalEntryRequest(
                openPeriod.getStartDate(),
                "REF-006",
                "Inactive account test",
                JournalSourceType.MANUAL,
                null,
                "AED",
                BigDecimal.ONE,
                List.of(
                        new JournalLineRequest(inactiveAccountId, null, "Inactive account debit", new BigDecimal("100.00"), BigDecimal.ZERO, null, null),
                        new JournalLineRequest(salesAccountId, null, "Credit sales", BigDecimal.ZERO, new BigDecimal("100.00"), null, null)
                ),
                true
        );

        BusinessException ex = assertThrows(BusinessException.class, () -> postingService.createJournalEntry(req));
        assertEquals("ACCOUNT_INACTIVE", ex.getCode());
    }

    @Test
    @Transactional
    @DisplayName("7. Tenant Isolation: Account belonging to another company must be rejected")
    void testCrossCompanyAccountRejected() {
        CreateJournalEntryRequest req = new CreateJournalEntryRequest(
                openPeriod.getStartDate(),
                "REF-007",
                "Cross-company account test",
                JournalSourceType.MANUAL,
                null,
                "AED",
                BigDecimal.ONE,
                List.of(
                        new JournalLineRequest(acmeAccountId, null, "ACME account debit", new BigDecimal("100.00"), BigDecimal.ZERO, null, null),
                        new JournalLineRequest(salesAccountId, null, "Credit sales", BigDecimal.ZERO, new BigDecimal("100.00"), null, null)
                ),
                true
        );

        BusinessException ex = assertThrows(BusinessException.class, () -> postingService.createJournalEntry(req));
        assertEquals("ACCOUNT_NOT_FOUND", ex.getCode());
    }

    @Test
    @Transactional
    @DisplayName("8. Tenant Isolation: Cost center belonging to another company must be rejected")
    void testCrossCompanyCostCenterRejected() {
        CreateJournalEntryRequest req = new CreateJournalEntryRequest(
                openPeriod.getStartDate(),
                "REF-008",
                "Cross-company cost center test",
                JournalSourceType.MANUAL,
                null,
                "AED",
                BigDecimal.ONE,
                List.of(
                        new JournalLineRequest(cashAccountId, acmeCostCenterId, "Debit with foreign CC", new BigDecimal("100.00"), BigDecimal.ZERO, null, null),
                        new JournalLineRequest(salesAccountId, null, "Credit sales", BigDecimal.ZERO, new BigDecimal("100.00"), null, null)
                ),
                true
        );

        BusinessException ex = assertThrows(BusinessException.class, () -> postingService.createJournalEntry(req));
        assertEquals("COST_CENTER_NOT_FOUND", ex.getCode());
    }

    @Test
    @Transactional
    @DisplayName("9. Period Control: Posting into CLOSED period must be rejected")
    void testPostingIntoClosedPeriodRejected() {
        CreateJournalEntryRequest req = new CreateJournalEntryRequest(
                closedPeriod.getStartDate(),
                "REF-009",
                "Closed period test",
                JournalSourceType.MANUAL,
                null,
                "AED",
                BigDecimal.ONE,
                List.of(
                        new JournalLineRequest(cashAccountId, null, "Debit cash", new BigDecimal("100.00"), BigDecimal.ZERO, null, null),
                        new JournalLineRequest(salesAccountId, null, "Credit sales", BigDecimal.ZERO, new BigDecimal("100.00"), null, null)
                ),
                true
        );

        BusinessException ex = assertThrows(BusinessException.class, () -> postingService.createJournalEntry(req));
        assertEquals("PERIOD_CLOSED", ex.getCode());
    }

    @Test
    @Transactional
    @DisplayName("10. Period Control: Posting into LOCKED period must be rejected")
    void testPostingIntoLockedPeriodRejected() {
        CreateJournalEntryRequest req = new CreateJournalEntryRequest(
                lockedPeriod.getStartDate(),
                "REF-010",
                "Locked period test",
                JournalSourceType.MANUAL,
                null,
                "AED",
                BigDecimal.ONE,
                List.of(
                        new JournalLineRequest(cashAccountId, null, "Debit cash", new BigDecimal("100.00"), BigDecimal.ZERO, null, null),
                        new JournalLineRequest(salesAccountId, null, "Credit sales", BigDecimal.ZERO, new BigDecimal("100.00"), null, null)
                ),
                true
        );

        BusinessException ex = assertThrows(BusinessException.class, () -> postingService.createJournalEntry(req));
        assertEquals("PERIOD_CLOSED", ex.getCode());
    }

    @Test
    @Transactional
    @DisplayName("11. Core Engine: Successful balanced multi-line posting generates gap-free JV sequence")
    void testSuccessfulPosting() {
        CreateJournalEntryRequest req = new CreateJournalEntryRequest(
                openPeriod.getStartDate(),
                "INV-1001",
                "Cash Sale with Cost Center",
                JournalSourceType.MANUAL,
                null,
                "AED",
                BigDecimal.ONE,
                List.of(
                        new JournalLineRequest(cashAccountId, bsbCostCenterId, "Cash in bank", new BigDecimal("500.00"), BigDecimal.ZERO, null, null),
                        new JournalLineRequest(salesAccountId, bsbCostCenterId, "Product Sales", BigDecimal.ZERO, new BigDecimal("500.00"), null, null)
                ),
                true
        );

        JournalEntryDto posted = postingService.createJournalEntry(req);
        assertNotNull(posted);
        assertNotNull(posted.id());
        assertNotNull(posted.entryNo());
        assertTrue(posted.entryNo().startsWith("JV-"));
        assertEquals(JournalStatus.POSTED, posted.status());
        assertEquals(0, new BigDecimal("500.00").compareTo(posted.totalDebit()));
        assertEquals(0, new BigDecimal("500.00").compareTo(posted.totalCredit()));
        assertEquals(2, posted.lines().size());
    }

    @Test
    @Transactional
    @DisplayName("12. Lifecycle: Attempting to post an already posted entry must be rejected")
    void testDuplicatePostingPrevention() {
        CreateJournalEntryRequest req = new CreateJournalEntryRequest(
                openPeriod.getStartDate(),
                "REF-012",
                "Draft to post test",
                JournalSourceType.MANUAL,
                null,
                "AED",
                BigDecimal.ONE,
                List.of(
                        new JournalLineRequest(cashAccountId, null, "Debit cash", new BigDecimal("200.00"), BigDecimal.ZERO, null, null),
                        new JournalLineRequest(salesAccountId, null, "Credit sales", BigDecimal.ZERO, new BigDecimal("200.00"), null, null)
                ),
                false // Create draft
        );

        JournalEntryDto draft = postingService.createJournalEntry(req);
        assertEquals(JournalStatus.DRAFT, draft.status());

        // First post succeeds
        JournalEntryDto posted = postingService.postJournalEntry(draft.id());
        assertEquals(JournalStatus.POSTED, posted.status());

        // Second post attempt must fail with ALREADY_POSTED
        BusinessException ex = assertThrows(BusinessException.class, () -> postingService.postJournalEntry(draft.id()));
        assertEquals("ALREADY_POSTED", ex.getCode());
    }

    @Test
    @Transactional
    @DisplayName("13. Reversal: Reversing entry swaps debits/credits, links reversal_of_id, and voids original")
    void testSuccessfulReversal() {
        CreateJournalEntryRequest req = new CreateJournalEntryRequest(
                openPeriod.getStartDate(),
                "ORIG-013",
                "Original transaction to reverse",
                JournalSourceType.MANUAL,
                null,
                "AED",
                BigDecimal.ONE,
                List.of(
                        new JournalLineRequest(cashAccountId, null, "Debit Cash", new BigDecimal("350.00"), BigDecimal.ZERO, null, null),
                        new JournalLineRequest(salesAccountId, null, "Credit Sales", BigDecimal.ZERO, new BigDecimal("350.00"), null, null)
                ),
                true
        );

        JournalEntryDto original = postingService.createJournalEntry(req);
        assertEquals(JournalStatus.POSTED, original.status());

        // Reverse
        JournalEntryDto reversal = postingService.reverseJournalEntry(original.id(), "Customer returned items");
        assertNotNull(reversal);
        assertEquals(JournalStatus.POSTED, reversal.status());
        assertEquals(original.id(), reversal.reversalOfId());
        assertTrue(reversal.narration().contains("Reversal of " + original.entryNo()));

        // Lines must be swapped: Cash now credit 350, Sales now debit 350
        JournalLineDto cashLine = reversal.lines().stream().filter(l -> l.accountId().equals(cashAccountId)).findFirst().orElseThrow();
        assertEquals(0, BigDecimal.ZERO.compareTo(cashLine.debit()));
        assertEquals(0, new BigDecimal("350.00").compareTo(cashLine.credit()));

        JournalLineDto salesLine = reversal.lines().stream().filter(l -> l.accountId().equals(salesAccountId)).findFirst().orElseThrow();
        assertEquals(0, new BigDecimal("350.00").compareTo(salesLine.debit()));
        assertEquals(0, BigDecimal.ZERO.compareTo(salesLine.credit()));

        // Original must be marked VOID
        JournalEntry originalInDb = journalEntryRepository.findById(original.id()).orElseThrow();
        assertEquals(JournalStatus.VOID, originalInDb.getStatus());
    }

    @Test
    @Transactional
    @DisplayName("14. Reversal: Attempting duplicate reversal must be rejected")
    void testDuplicateReversalPrevention() {
        CreateJournalEntryRequest req = new CreateJournalEntryRequest(
                openPeriod.getStartDate(),
                "ORIG-014",
                "Entry for duplicate reversal test",
                JournalSourceType.MANUAL,
                null,
                "AED",
                BigDecimal.ONE,
                List.of(
                        new JournalLineRequest(cashAccountId, null, "Debit Cash", new BigDecimal("150.00"), BigDecimal.ZERO, null, null),
                        new JournalLineRequest(salesAccountId, null, "Credit Sales", BigDecimal.ZERO, new BigDecimal("150.00"), null, null)
                ),
                true
        );

        JournalEntryDto original = postingService.createJournalEntry(req);

        // First reversal succeeds
        postingService.reverseJournalEntry(original.id(), "Initial reversal");

        // Attempting to reverse original again must fail
        BusinessException ex = assertThrows(BusinessException.class, () ->
                postingService.reverseJournalEntry(original.id(), "Duplicate attempt"));
        assertEquals("CANNOT_REVERSE_UNPOSTED", ex.getCode());
    }

    @Test
    @Transactional
    @DisplayName("15. General Ledger Report: Correctly computes opening, movements, and running balance")
    void testGeneralLedgerReportAccuracy() {
        LocalDate jan10 = openPeriod.getStartDate().plusDays(9);
        LocalDate jan15 = openPeriod.getStartDate().plusDays(14);

        // Post Entry 1 on Jan 10: Cash +1000, Sales +1000
        postingService.createJournalEntry(new CreateJournalEntryRequest(
                jan10, "DOC-1", "Sale 1", JournalSourceType.MANUAL, null, "AED", BigDecimal.ONE,
                List.of(
                        new JournalLineRequest(cashAccountId, null, "Cash 1", new BigDecimal("1000.00"), BigDecimal.ZERO, null, null),
                        new JournalLineRequest(salesAccountId, null, "Sales 1", BigDecimal.ZERO, new BigDecimal("1000.00"), null, null)
                ),
                true
        ));

        // Post Entry 2 on Jan 15: Cash +500, Sales +500
        postingService.createJournalEntry(new CreateJournalEntryRequest(
                jan15, "DOC-2", "Sale 2", JournalSourceType.MANUAL, null, "AED", BigDecimal.ONE,
                List.of(
                        new JournalLineRequest(cashAccountId, null, "Cash 2", new BigDecimal("500.00"), BigDecimal.ZERO, null, null),
                        new JournalLineRequest(salesAccountId, null, "Sales 2", BigDecimal.ZERO, new BigDecimal("500.00"), null, null)
                ),
                true
        ));

        // Run report from Jan 12 to Jan 20
        GeneralLedgerReportDto report = reportService.getGeneralLedgerReport(
                cashAccountId,
                jan10.plusDays(2), // from Jan 12 (opening should include Jan 10 entry = 1000)
                jan15.plusDays(2)  // to Jan 17
        );

        assertNotNull(report);
        assertEquals(0, new BigDecimal("1000.00").compareTo(report.openingBalance()));
        assertEquals(1, report.lines().size()); // Only Jan 15 entry
        assertEquals(0, new BigDecimal("500.00").compareTo(report.lines().get(0).debit()));
        assertEquals(0, new BigDecimal("1500.00").compareTo(report.closingBalance()));
    }

    @Test
    @Transactional
    @DisplayName("16. Trial Balance Report: Accurately balances total debits and credits across all accounts")
    void testTrialBalanceReportAccuracy() {
        // Post transaction: Cash 1200, Sales 1200
        postingService.createJournalEntry(new CreateJournalEntryRequest(
                openPeriod.getStartDate(),
                "TB-01",
                "Trial Balance Seed",
                JournalSourceType.MANUAL,
                null,
                "AED",
                BigDecimal.ONE,
                List.of(
                        new JournalLineRequest(cashAccountId, null, "Cash in", new BigDecimal("1200.00"), BigDecimal.ZERO, null, null),
                        new JournalLineRequest(salesAccountId, null, "Sales", BigDecimal.ZERO, new BigDecimal("1200.00"), null, null)
                ),
                true
        ));

        TrialBalanceReportDto tb = reportService.getTrialBalanceReport(openPeriod.getEndDate());
        assertNotNull(tb);
        assertTrue(tb.isBalanced());
        assertEquals(0, tb.totalDebit().compareTo(tb.totalCredit()));
        assertFalse(tb.lines().isEmpty());
    }

    @Test
    @Transactional
    @DisplayName("17. Cross-Company Isolation: Company B's reports never include Company A's transactions")
    void testCrossCompanyReportIsolation() {
        // Post in Company BSB
        postingService.createJournalEntry(new CreateJournalEntryRequest(
                openPeriod.getStartDate(),
                "BSB-TX",
                "BSB Private Transaction",
                JournalSourceType.MANUAL,
                null,
                "AED",
                BigDecimal.ONE,
                List.of(
                        new JournalLineRequest(cashAccountId, null, "Cash BSB", new BigDecimal("888.00"), BigDecimal.ZERO, null, null),
                        new JournalLineRequest(salesAccountId, null, "Sales BSB", BigDecimal.ZERO, new BigDecimal("888.00"), null, null)
                ),
                true
        ));

        // Switch to Company ACME
        authenticateAs(2L, acmeCompanyId, "acme_admin@finledger.com", Set.of(
                "REPORT_VIEW", "LEDGER_VIEW", "ACCOUNT_VIEW"
        ));

        // Company ACME query on BSB Cash account must fail with ACCOUNT_NOT_FOUND
        BusinessException ex = assertThrows(BusinessException.class, () ->
                reportService.getGeneralLedgerReport(cashAccountId, null, null));
        assertEquals("ACCOUNT_NOT_FOUND", ex.getCode());

        // ACME Trial balance must not reflect BSB's 888.00 transaction
        TrialBalanceReportDto acmeTb = reportService.getTrialBalanceReport(openPeriod.getEndDate());
        assertNotNull(acmeTb);
        assertTrue(acmeTb.lines().stream().noneMatch(l -> l.accountId().equals(cashAccountId)));
    }
}

