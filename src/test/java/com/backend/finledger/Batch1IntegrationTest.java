package com.backend.finledger;

import com.backend.finledger.banking.entity.AccountKind;
import com.backend.finledger.banking.entity.BankAccount;
import com.backend.finledger.banking.entity.BankReconciliation;
import com.backend.finledger.banking.entity.BankStatement;
import com.backend.finledger.banking.entity.BankStatementLine;
import com.backend.finledger.banking.entity.ReconciliationStatus;
import com.backend.finledger.banking.repository.BankAccountRepository;
import com.backend.finledger.banking.repository.BankReconciliationRepository;
import com.backend.finledger.banking.repository.BankStatementLineRepository;
import com.backend.finledger.banking.repository.BankStatementRepository;
import com.backend.finledger.banking.service.BankReconciliationService;
import com.backend.finledger.common.BusinessException;
import com.backend.finledger.common.ForbiddenException;
import com.backend.finledger.company.entity.Company;
import com.backend.finledger.company.repository.CompanyRepository;
import com.backend.finledger.inventory.entity.Item;
import com.backend.finledger.inventory.entity.ItemType;
import com.backend.finledger.inventory.entity.MovementType;
import com.backend.finledger.inventory.entity.StockSourceType;
import com.backend.finledger.inventory.entity.Warehouse;
import com.backend.finledger.inventory.repository.ItemRepository;
import com.backend.finledger.inventory.repository.StockBalanceRepository;
import com.backend.finledger.inventory.repository.WarehouseRepository;
import com.backend.finledger.inventory.service.StockService;
import com.backend.finledger.ledger.dto.CreateJournalEntryRequest;
import com.backend.finledger.ledger.dto.JournalEntryDto;
import com.backend.finledger.ledger.dto.JournalLineRequest;
import com.backend.finledger.ledger.entity.Account;
import com.backend.finledger.ledger.entity.FiscalPeriod;
import com.backend.finledger.ledger.entity.FiscalPeriodStatus;
import com.backend.finledger.ledger.entity.JournalLine;
import com.backend.finledger.ledger.entity.JournalSourceType;
import com.backend.finledger.ledger.repository.AccountRepository;
import com.backend.finledger.ledger.repository.FiscalPeriodRepository;
import com.backend.finledger.ledger.repository.FiscalYearRepository;
import com.backend.finledger.ledger.repository.JournalLineRepository;
import com.backend.finledger.ledger.service.PostingService;
import com.backend.finledger.report.dto.BalanceSheetReportDto;
import com.backend.finledger.report.dto.ProfitLossReportDto;
import com.backend.finledger.report.service.ReportService;
import com.backend.finledger.security.UserPrincipal;
import org.junit.jupiter.api.AfterEach;
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

@DisplayName("Batch 1 Integration Verification (Flyway V6, Tenant Isolation, P&L CLOSING, Reconciled Void)")
class Batch1IntegrationTest extends BaseIntegrationTest {

    @Autowired private CompanyRepository companyRepository;
    @Autowired private AccountRepository accountRepository;
    @Autowired private FiscalYearRepository fiscalYearRepository;
    @Autowired private FiscalPeriodRepository fiscalPeriodRepository;
    @Autowired private PostingService postingService;
    @Autowired private ReportService reportService;
    @Autowired private JournalLineRepository journalLineRepository;
    @Autowired private ItemRepository itemRepository;
    @Autowired private WarehouseRepository warehouseRepository;
    @Autowired private StockBalanceRepository stockBalanceRepository;
    @Autowired private StockService stockService;
    @Autowired private BankAccountRepository bankAccountRepository;
    @Autowired private BankStatementRepository bankStatementRepository;
    @Autowired private BankStatementLineRepository bankStatementLineRepository;
    @Autowired private BankReconciliationRepository bankReconciliationRepository;
    @Autowired private BankReconciliationService bankReconciliationService;

    private void authenticateAs(Long companyId, Set<String> permissions) {
        UserPrincipal principal = new UserPrincipal(
                1L, companyId, "batch1@finledger.com", "", "Batch1 Admin", true, permissions);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    @AfterEach
    void clearAuth() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @Transactional
    @DisplayName("Should preserve Profit & Loss revenue/expense after CLOSING journal entry and classify Balance Sheet properly")
    void testProfitLossExcludesClosingEntriesAndBalanceSheetClassification() {
        Company bsb = companyRepository.findByCode("BSB").orElseThrow();
        Long cid = bsb.getId();

        authenticateAs(cid, Set.of("JOURNAL_CREATE", "JOURNAL_POST", "JOURNAL_VOID", "REPORT_VIEW", "LEDGER_VIEW"));

        Account cash = accountRepository.findByCompanyIdAndCode(cid, "1110").orElseThrow();
        Account sales = accountRepository.findByCompanyIdAndCode(cid, "4100").orElseThrow();
        Account cogs = accountRepository.findByCompanyIdAndCode(cid, "5100").orElseThrow();
        Account retained = accountRepository.findByCompanyIdAndCode(cid, "3200").orElseThrow();

        assertNotNull(accountRepository.findByCompanyIdAndCode(cid, "4900").orElse(null),
                "Seeded FX Gain account 4900 should exist");
        assertNotNull(accountRepository.findByCompanyIdAndCode(cid, "6200").orElse(null),
                "Seeded FX Loss account 6200 should exist");

        FiscalPeriod openPeriod = fiscalYearRepository.findByCompanyIdAndName(cid, "FY" + LocalDate.now().getYear())
                .flatMap(fy -> fiscalPeriodRepository.findByFiscalYearIdOrderByPeriodNoAsc(fy.getId()).stream()
                        .filter(p -> p.getStatus() == FiscalPeriodStatus.OPEN)
                        .findFirst())
                .orElseThrow();

        LocalDate txDate = openPeriod.getStartDate();

        // Post operating revenue 1000 & COGS 400
        postingService.createJournalEntry(new CreateJournalEntryRequest(
                txDate, "B1-OP-1", "Operating revenue", JournalSourceType.MANUAL, null, "AED", BigDecimal.ONE,
                List.of(
                        new JournalLineRequest(cash.getId(), null, "Cash", new BigDecimal("1000.00"), BigDecimal.ZERO, null, null),
                        new JournalLineRequest(sales.getId(), null, "Sales", BigDecimal.ZERO, new BigDecimal("1000.00"), null, null)
                ),
                true
        ));
        postingService.createJournalEntry(new CreateJournalEntryRequest(
                txDate, "B1-OP-2", "Operating COGS", JournalSourceType.MANUAL, null, "AED", BigDecimal.ONE,
                List.of(
                        new JournalLineRequest(cogs.getId(), null, "COGS", new BigDecimal("400.00"), BigDecimal.ZERO, null, null),
                        new JournalLineRequest(cash.getId(), null, "Cash", BigDecimal.ZERO, new BigDecimal("400.00"), null, null)
                ),
                true
        ));

        // Post year-end CLOSING journal entry zeroing Revenue/COGS into Retained Earnings
        postingService.createJournalEntry(new CreateJournalEntryRequest(
                txDate, "YEC-TEST", "Closing test", JournalSourceType.CLOSING, null, "AED", BigDecimal.ONE,
                List.of(
                        new JournalLineRequest(sales.getId(), null, "Close sales", new BigDecimal("1000.00"), BigDecimal.ZERO, null, null),
                        new JournalLineRequest(cogs.getId(), null, "Close COGS", BigDecimal.ZERO, new BigDecimal("400.00"), null, null),
                        new JournalLineRequest(retained.getId(), null, "To Retained Earnings", BigDecimal.ZERO, new BigDecimal("600.00"), null, null)
                ),
                true
        ));

        ProfitLossReportDto pnl = reportService.getProfitLossReport(openPeriod.getStartDate(), openPeriod.getEndDate(), null, null);
        assertEquals(0, new BigDecimal("1000.00").compareTo(pnl.totalRevenue()), "P&L must exclude CLOSING entries");
        assertEquals(0, new BigDecimal("400.00").compareTo(pnl.totalCogs()), "P&L COGS must exclude CLOSING entries");
        assertEquals(0, new BigDecimal("600.00").compareTo(pnl.netProfit()));

        BalanceSheetReportDto bs = reportService.getBalanceSheetReport(openPeriod.getEndDate(), null);
        assertTrue(bs.balanced(), "Balance Sheet must remain balanced after CLOSING entry");
        assertTrue(bs.equityLines().stream().noneMatch(l -> "3200".equals(l.accountCode())),
                "3200 Retained Earnings must not be duplicated in equityLines");
    }

    @Test
    @Transactional
    @DisplayName("Should enforce tenant isolation on StockBalance, BankReconciliation, BankStatement, and block reversing reconciled journals")
    void testChildTableTenantIsolationAndReconciledVoidBlock() {
        Company bsb = companyRepository.findByCode("BSB").orElseThrow();
        Company other = new Company();
        other.setCode("B1OTHER");
        other.setLegalName("Batch1 Other Tenant LLC");
        other.setCountryCode("AE");
        other.setBaseCurrency("AED");
        other.setIsActive(true);
        other = companyRepository.saveAndFlush(other);

        Item item = new Item();
        item.setCompanyId(bsb.getId());
        item.setSku("B1-SKU-1");
        item.setName("Batch1 Item");
        item.setItemType(ItemType.INVENTORY);
        item.setUnit("PCS");
        item.setIsActive(true);
        item = itemRepository.saveAndFlush(item);

        Warehouse wh = warehouseRepository.findFirstByCompanyIdAndIsDefaultTrue(bsb.getId()).orElseThrow();
        stockService.receiveStock(bsb.getId(), item.getId(), wh.getId(),
                new BigDecimal("5"), new BigDecimal("10.00"),
                MovementType.OPENING, StockSourceType.OPENING, 1L, 1L, null);

        assertTrue(stockBalanceRepository.findByCompanyIdAndItemIdAndWarehouseId(bsb.getId(), item.getId(), wh.getId()).isPresent());
        assertTrue(stockBalanceRepository.findByCompanyIdAndItemIdAndWarehouseId(other.getId(), item.getId(), wh.getId()).isEmpty());

        authenticateAs(other.getId(), Set.of("ITEM_VIEW", "BANK_RECONCILE"));
        Long itemId = item.getId();
        Long whId = wh.getId();
        assertThrows(ForbiddenException.class, () -> stockService.getStockBalance(itemId, whId));

        BankAccount bsbBank = bankAccountRepository.findByCompanyIdAndIsActiveTrue(bsb.getId()).stream().findFirst().orElseThrow();
        BankStatement stmt = new BankStatement();
        stmt.setBankAccountId(bsbBank.getId());
        stmt.setStatementDate(LocalDate.of(2099, 1, 15));
        stmt.setOpeningBalance(BigDecimal.ZERO);
        stmt.setClosingBalance(BigDecimal.ZERO);
        stmt = bankStatementRepository.saveAndFlush(stmt);

        BankStatementLine stmtLine = new BankStatementLine();
        stmtLine.setBankStatement(stmt);
        stmtLine.setTxnDate(LocalDate.of(2099, 1, 15));
        stmtLine.setAmount(new BigDecimal("100.00"));
        stmtLine = bankStatementLineRepository.saveAndFlush(stmtLine);

        BankReconciliation recon = new BankReconciliation();
        recon.setBankAccountId(bsbBank.getId());
        recon.setStatementDate(LocalDate.of(2099, 1, 16));
        recon.setStatus(ReconciliationStatus.IN_PROGRESS);
        recon = bankReconciliationRepository.saveAndFlush(recon);

        Long stmtId = stmt.getId();
        Long lineId = stmtLine.getId();
        Long reconId = recon.getId();

        assertTrue(bankStatementRepository.findByCompanyIdAndId(other.getId(), stmtId).isEmpty());
        assertTrue(bankStatementLineRepository.findByCompanyIdAndId(other.getId(), lineId).isEmpty());
        assertTrue(bankReconciliationRepository.findByCompanyIdAndId(other.getId(), reconId).isEmpty());
        assertThrows(ForbiddenException.class, () -> bankReconciliationService.getReconciliationById(reconId));

        // Reconciled JournalLine blocks reverseJournalEntry
        authenticateAs(bsb.getId(), Set.of("JOURNAL_CREATE", "JOURNAL_POST", "JOURNAL_VOID"));
        Account cash = accountRepository.findByCompanyIdAndCode(bsb.getId(), "1110").orElseThrow();
        Account sales = accountRepository.findByCompanyIdAndCode(bsb.getId(), "4100").orElseThrow();
        FiscalPeriod openPeriod = fiscalYearRepository.findByCompanyIdAndName(bsb.getId(), "FY" + LocalDate.now().getYear())
                .flatMap(fy -> fiscalPeriodRepository.findByFiscalYearIdOrderByPeriodNoAsc(fy.getId()).stream()
                        .filter(p -> p.getStatus() == FiscalPeriodStatus.OPEN)
                        .findFirst())
                .orElseThrow();

        JournalEntryDto je = postingService.createJournalEntry(new CreateJournalEntryRequest(
                openPeriod.getStartDate(), "REC-TEST", "Reconciled JE", JournalSourceType.MANUAL, null, "AED", BigDecimal.ONE,
                List.of(
                        new JournalLineRequest(cash.getId(), null, "Cash", new BigDecimal("100.00"), BigDecimal.ZERO, null, null),
                        new JournalLineRequest(sales.getId(), null, "Sales", BigDecimal.ZERO, new BigDecimal("100.00"), null, null)
                ),
                true
        ));
        List<JournalLine> jlList = journalLineRepository.findByJournalEntryIdOrderByLineNoAsc(je.id());
        jlList.get(0).setIsReconciled(true);
        journalLineRepository.saveAndFlush(jlList.get(0));

        BusinessException recEx = assertThrows(BusinessException.class,
                () -> postingService.reverseJournalEntry(je.id(), "Should be blocked"));
        assertEquals("JOURNAL_RECONCILED", recEx.getCode());
    }
}
