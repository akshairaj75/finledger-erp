package com.backend.finledger.report;

import com.backend.finledger.banking.entity.BankAccount;
import com.backend.finledger.banking.repository.BankAccountRepository;
import com.backend.finledger.crm.entity.CrmPipelineStage;
import com.backend.finledger.crm.entity.Opportunity;
import com.backend.finledger.crm.entity.StageType;
import com.backend.finledger.crm.repository.CrmPipelineStageRepository;
import com.backend.finledger.crm.repository.OpportunityRepository;
import com.backend.finledger.expense.repository.ExpenseCategoryRepository;
import com.backend.finledger.expense.repository.ExpenseRepository;
import com.backend.finledger.inventory.entity.*;
import com.backend.finledger.inventory.repository.*;
import com.backend.finledger.ledger.dto.GeneralLedgerReportDto;
import com.backend.finledger.ledger.dto.TrialBalanceReportDto;
import com.backend.finledger.ledger.entity.*;
import com.backend.finledger.report.dto.StockMovementLineDto;
import com.backend.finledger.report.dto.StockMovementReportDto;
import com.backend.finledger.ledger.repository.AccountRepository;
import com.backend.finledger.ledger.repository.JournalEntryRepository;
import com.backend.finledger.ledger.repository.JournalLineRepository;
import com.backend.finledger.purchases.dto.ApAgeingReportDto;
import com.backend.finledger.purchases.repository.BillRepository;
import com.backend.finledger.purchases.service.ApAgeingService;
import com.backend.finledger.report.dto.BalanceSheetReportDto;
import com.backend.finledger.report.dto.DashboardSummaryDto;
import com.backend.finledger.report.dto.ProfitLossReportDto;
import com.backend.finledger.report.service.impl.ReportServiceImpl;
import com.backend.finledger.sales.dto.ArAgeingReportDto;
import com.backend.finledger.sales.repository.InvoiceRepository;
import com.backend.finledger.sales.service.ArAgeingService;
import com.backend.finledger.security.CurrentUser;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardAndReportsTest {

    @Mock private AccountRepository accountRepository;
    @Mock private JournalLineRepository journalLineRepository;
    @Mock private JournalEntryRepository journalEntryRepository;
    @Mock private BankAccountRepository bankAccountRepository;
    @Mock private OpportunityRepository opportunityRepository;
    @Mock private CrmPipelineStageRepository stageRepository;
    @Mock private StockBalanceRepository stockBalanceRepository;
    @Mock private StockMovementRepository stockMovementRepository;
    @Mock private ItemRepository itemRepository;
    @Mock private ItemCategoryRepository itemCategoryRepository;
    @Mock private WarehouseRepository warehouseRepository;
    @Mock private ExpenseRepository expenseRepository;
    @Mock private ExpenseCategoryRepository expenseCategoryRepository;
    @Mock private ArAgeingService arAgeingService;
    @Mock private ApAgeingService apAgeingService;
    @Mock private InvoiceRepository invoiceRepository;
    @Mock private BillRepository billRepository;

    @InjectMocks
    private ReportServiceImpl reportService;

    private MockedStatic<CurrentUser> currentUserMock;

    @BeforeEach
    void setUp() {
        currentUserMock = Mockito.mockStatic(CurrentUser.class);
        currentUserMock.when(CurrentUser::getCompanyId).thenReturn(1L);
    }

    @AfterEach
    void tearDown() {
        currentUserMock.close();
    }

    @Test
    @DisplayName("Should aggregate Dashboard KPIs successfully")
    void testGetDashboardSummary() {
        Account revAcc = new Account();
        revAcc.setId(401L);
        revAcc.setCode("4000");
        revAcc.setName("Sales Revenue");
        revAcc.setAccountType(AccountType.INCOME);

        Account expAcc = new Account();
        expAcc.setId(501L);
        expAcc.setCode("5000");
        expAcc.setName("Cost of Goods Sold");
        expAcc.setAccountType(AccountType.EXPENSE);

        when(accountRepository.findByCompanyIdOrderByCodeAsc(1L)).thenReturn(List.of(revAcc, expAcc));

        JournalLineRepository.AccountBalanceSummary revSummary = mock(JournalLineRepository.AccountBalanceSummary.class);
        when(revSummary.getAccountId()).thenReturn(401L);
        when(revSummary.getTotalDebit()).thenReturn(BigDecimal.ZERO);
        when(revSummary.getTotalCredit()).thenReturn(BigDecimal.valueOf(50000));

        JournalLineRepository.AccountBalanceSummary expSummary = mock(JournalLineRepository.AccountBalanceSummary.class);
        when(expSummary.getAccountId()).thenReturn(501L);
        when(expSummary.getTotalDebit()).thenReturn(BigDecimal.valueOf(20000));
        when(expSummary.getTotalCredit()).thenReturn(BigDecimal.ZERO);

        when(journalLineRepository.getAccountBalanceSummariesBetweenExcludingSourceType(
                eq(1L), any(LocalDate.class), any(LocalDate.class), eq(null), eq(JournalSourceType.CLOSING)))
                .thenReturn(List.of(revSummary, expSummary));

        ArAgeingReportDto arDto = new ArAgeingReportDto(
                LocalDate.now(), Collections.emptyList(),
                BigDecimal.valueOf(10000), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.valueOf(10000)
        );
        when(arAgeingService.getArAgeingReport(any())).thenReturn(arDto);

        ApAgeingReportDto apDto = new ApAgeingReportDto(
                LocalDate.now(), Collections.emptyList(),
                BigDecimal.valueOf(5000), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.valueOf(5000)
        );
        when(apAgeingService.getApAgeingReport(any())).thenReturn(apDto);

        when(bankAccountRepository.findByCompanyIdAndIsActiveTrue(1L)).thenReturn(Collections.emptyList());
        when(journalLineRepository.getAccountBalanceSummaries(eq(1L), any())).thenReturn(Collections.emptyList());

        Opportunity opp = new Opportunity();
        opp.setStageId(1L);
        opp.setExpectedValue(BigDecimal.valueOf(75000));
        when(opportunityRepository.findByCompanyId(1L)).thenReturn(List.of(opp));

        CrmPipelineStage stage = new CrmPipelineStage(1L, "Discovery", (byte) 1, (byte) 20, StageType.OPEN);
        stage.setId(1L);
        when(stageRepository.findByCompanyIdOrderBySortOrderAsc(1L)).thenReturn(List.of(stage));

        when(itemRepository.findByCompanyIdAndIsActiveTrue(1L)).thenReturn(Collections.emptyList());

        when(expenseRepository.countAwaitingApproval(1L)).thenReturn(2L);
        when(expenseRepository.searchExpenses(eq(1L), any(), any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(Collections.emptyList()));

        when(journalEntryRepository.searchJournalEntries(eq(1L), any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(Collections.emptyList()));

        DashboardSummaryDto summary = reportService.getDashboardSummary(null, null);

        assertNotNull(summary);
        assertEquals(BigDecimal.valueOf(50000), summary.totalRevenueYtd());
        assertEquals(BigDecimal.valueOf(20000), summary.totalExpensesYtd());
        assertEquals(BigDecimal.valueOf(10000), summary.accountsReceivable());
        assertEquals(BigDecimal.valueOf(5000), summary.accountsPayable());
        assertEquals(BigDecimal.valueOf(75000), summary.openPipelineValue());
        assertEquals(1, summary.openPipelineCount());
        assertEquals(2, summary.expensesAwaitingApprovalCount());
    }

    @Test
    @DisplayName("Should generate Profit and Loss report correctly")
    void testGetProfitLossReport() throws Exception {
        Account revAcc = new Account();
        revAcc.setId(401L);
        revAcc.setCode("4000");
        revAcc.setName("Service Revenue");
        revAcc.setAccountType(AccountType.INCOME);

        Account cogsAcc = new Account();
        cogsAcc.setId(501L);
        cogsAcc.setCode("5000");
        cogsAcc.setName("Purchases COGS");
        cogsAcc.setAccountType(AccountType.EXPENSE);
        cogsAcc.setSubType("COST_OF_SALES");

        Account opexAcc = new Account();
        opexAcc.setId(601L);
        opexAcc.setCode("6100");
        opexAcc.setName("Rent Expense");
        opexAcc.setAccountType(AccountType.EXPENSE);
        opexAcc.setSubType("OPERATING_EXPENSE");

        when(accountRepository.findByCompanyIdOrderByCodeAsc(1L)).thenReturn(List.of(revAcc, cogsAcc, opexAcc));

        JournalLineRepository.AccountBalanceSummary s1 = mock(JournalLineRepository.AccountBalanceSummary.class);
        when(s1.getAccountId()).thenReturn(401L);
        when(s1.getTotalDebit()).thenReturn(BigDecimal.ZERO);
        when(s1.getTotalCredit()).thenReturn(BigDecimal.valueOf(100000));

        JournalLineRepository.AccountBalanceSummary s2 = mock(JournalLineRepository.AccountBalanceSummary.class);
        when(s2.getAccountId()).thenReturn(501L);
        when(s2.getTotalDebit()).thenReturn(BigDecimal.valueOf(40000));
        when(s2.getTotalCredit()).thenReturn(BigDecimal.ZERO);

        JournalLineRepository.AccountBalanceSummary s3 = mock(JournalLineRepository.AccountBalanceSummary.class);
        when(s3.getAccountId()).thenReturn(601L);
        when(s3.getTotalDebit()).thenReturn(BigDecimal.valueOf(25000));
        when(s3.getTotalCredit()).thenReturn(BigDecimal.ZERO);

        when(journalLineRepository.getAccountBalanceSummariesBetweenExcludingSourceType(
                eq(1L), any(), any(), eq(null), eq(JournalSourceType.CLOSING)))
                .thenReturn(List.of(s1, s2, s3));

        ProfitLossReportDto pnl = reportService.getProfitLossReport(
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31), null, null);

        assertNotNull(pnl);
        assertEquals(BigDecimal.valueOf(100000), pnl.totalRevenue());
        assertEquals(BigDecimal.valueOf(40000), pnl.totalCogs());
        assertEquals(BigDecimal.valueOf(60000), pnl.grossProfit());
        assertEquals(BigDecimal.valueOf(25000), pnl.totalExpenses());
        assertEquals(BigDecimal.valueOf(35000), pnl.netProfit());

        byte[] pnlXlsx = reportService.exportReport("profit-loss", "xlsx",
                Map.of("from", "2026-01-01", "to", "2026-12-31"));
        assertNotNull(pnlXlsx);
        try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(pnlXlsx))) {
            Sheet sheet = wb.getSheet("Profit & Loss");
            assertNotNull(sheet, "Sheet tab name should be 'Profit & Loss'");
            assertNotNull(sheet.getPaneInformation(), "Freeze pane should be configured");
            assertTrue(sheet.getPaneInformation().isFreezePane());

            boolean foundGrossProfitFormula = false;
            boolean foundNetProfitFormula = false;
            boolean foundNumericRevenue = false;

            for (Row row : sheet) {
                Cell nameCell = row.getCell(1);
                Cell amountCell = row.getCell(2);
                if (nameCell != null && nameCell.getCellType() == CellType.STRING) {
                    String label = nameCell.getStringCellValue().trim();
                    if (label.contains("Service Revenue") && amountCell != null) {
                        assertEquals(CellType.NUMERIC, amountCell.getCellType(), "Account amount must be NUMERIC");
                        assertEquals(100000.0, amountCell.getNumericCellValue(), 0.001);
                        foundNumericRevenue = true;
                    } else if (label.equalsIgnoreCase("Gross Profit") && amountCell != null) {
                        assertEquals(CellType.FORMULA, amountCell.getCellType(), "Gross Profit must use FORMULA");
                        foundGrossProfitFormula = true;
                    } else if (label.equalsIgnoreCase("NET PROFIT / (LOSS)") && amountCell != null) {
                        assertEquals(CellType.FORMULA, amountCell.getCellType(), "Net Profit must use FORMULA");
                        foundNetProfitFormula = true;
                    }
                }
            }
            assertTrue(foundNumericRevenue, "Account line amount must be numeric");
            assertTrue(foundGrossProfitFormula, "Gross Profit should use live formula");
            assertTrue(foundNetProfitFormula, "Net Profit should use live formula");
        }
    }

    @Test
    @DisplayName("Should export Balance Sheet Excel and Profit & Loss PDF with forwarded date filters")
    void testReportExportAllTypesWithParams() throws Exception {
        Account cashAcc = new Account();
        cashAcc.setId(101L);
        cashAcc.setCode("1110");
        cashAcc.setName("Bank");
        cashAcc.setAccountType(AccountType.ASSET);
        cashAcc.setSubType("BANK");
        cashAcc.setNormalBalance(NormalBalance.DEBIT);

        when(accountRepository.findByCompanyIdOrderByCodeAsc(1L)).thenReturn(List.of(cashAcc));
        when(journalLineRepository.getAccountBalanceSummaries(eq(1L), eq(LocalDate.of(2026, 10, 31))))
                .thenReturn(Collections.emptyList());
        when(journalLineRepository.getAccountBalanceSummariesBetween(
                eq(1L), any(LocalDate.class), any(LocalDate.class), eq(null)))
                .thenReturn(Collections.emptyList());

        byte[] xlsxBytes = reportService.exportReport("balance-sheet", "xlsx", Map.of("asOf", "2026-10-31"));
        assertNotNull(xlsxBytes);
        assertTrue(xlsxBytes.length > 0);
        // XLSX files begin with PK zip magic header
        assertEquals('P', (char) xlsxBytes[0]);
        assertEquals('K', (char) xlsxBytes[1]);

        try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(xlsxBytes))) {
            Sheet sheet = wb.getSheet("Balance Sheet");
            assertNotNull(sheet, "Sheet tab name should be 'Balance Sheet'");
            assertEquals("FinLedger ERP", sheet.getRow(0).getCell(0).getStringCellValue());
            assertEquals("Statement of Financial Position (Balance Sheet)", sheet.getRow(1).getCell(0).getStringCellValue());
            assertTrue(sheet.getRow(2).getCell(0).getStringCellValue().contains("As of 31-Oct-2026"));
            assertNotNull(sheet.getPaneInformation(), "Freeze pane should be configured");
            assertTrue(sheet.getPaneInformation().isFreezePane());
        }

        byte[] pdfBytes = reportService.exportReport("profit-loss", "pdf",
                Map.of("from", "2026-01-01", "to", "2026-10-31"));
        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 0);
        assertEquals("%PDF", new String(pdfBytes, 0, 4));

        byte[] bsPdf = reportService.exportReport("balance-sheet", "pdf", Map.of("asOf", "2026-10-31"));
        assertEquals("%PDF", new String(bsPdf, 0, 4));

        byte[] tbPdf = reportService.exportReport("trial-balance", "pdf", Map.of("asOf", "2026-10-31"));
        assertEquals("%PDF", new String(tbPdf, 0, 4));
    }

    @Test
    @DisplayName("Should classify Balance Sheet current accounts properly without duplicating Retained Earnings")
    void testGetBalanceSheetClassificationAndRetainedEarnings() throws Exception {
        Account ar = new Account(1L, "1200", "Accounts Receivable", AccountType.ASSET, "CURRENT_ASSET", null, false, NormalBalance.DEBIT);
        ar.setId(12L);
        Account inv = new Account(1L, "1300", "Inventory", AccountType.ASSET, "STOCK", null, false, NormalBalance.DEBIT);
        inv.setId(13L);
        Account vatIn = new Account(1L, "1400", "VAT Recoverable (Input)", AccountType.ASSET, "TAX", null, false, NormalBalance.DEBIT);
        vatIn.setId(14L);
        Account fixedAsset = new Account(1L, "1500", "Equipment", AccountType.ASSET, "FIXED_ASSET", null, false, NormalBalance.DEBIT);
        fixedAsset.setId(15L);

        Account vatOut = new Account(1L, "2200", "VAT Payable (Output)", AccountType.LIABILITY, "TAX", null, false, NormalBalance.CREDIT);
        vatOut.setId(22L);
        Account accrued = new Account(1L, "2300", "Accrued Expenses", AccountType.LIABILITY, "ACCRUAL", null, false, NormalBalance.CREDIT);
        accrued.setId(23L);

        Account capital = new Account(1L, "3100", "Owner's Capital", AccountType.EQUITY, "EQUITY", null, false, NormalBalance.CREDIT);
        capital.setId(31L);
        Account retained = new Account(1L, "3200", "Retained Earnings", AccountType.EQUITY, "EQUITY", null, false, NormalBalance.CREDIT);
        retained.setId(32L);

        when(accountRepository.findByCompanyIdOrderByCodeAsc(1L))
                .thenReturn(List.of(ar, inv, vatIn, fixedAsset, vatOut, accrued, capital, retained));

        JournalLineRepository.AccountBalanceSummary sAr = mockSummary(12L, BigDecimal.valueOf(1000), BigDecimal.ZERO);
        JournalLineRepository.AccountBalanceSummary sInv = mockSummary(13L, BigDecimal.valueOf(2000), BigDecimal.ZERO);
        JournalLineRepository.AccountBalanceSummary sVatIn = mockSummary(14L, BigDecimal.valueOf(500), BigDecimal.ZERO);
        JournalLineRepository.AccountBalanceSummary sFix = mockSummary(15L, BigDecimal.valueOf(5000), BigDecimal.ZERO);
        JournalLineRepository.AccountBalanceSummary sVatOut = mockSummary(22L, BigDecimal.ZERO, BigDecimal.valueOf(800));
        JournalLineRepository.AccountBalanceSummary sAccr = mockSummary(23L, BigDecimal.ZERO, BigDecimal.valueOf(700));
        JournalLineRepository.AccountBalanceSummary sCap = mockSummary(31L, BigDecimal.ZERO, BigDecimal.valueOf(4000));
        JournalLineRepository.AccountBalanceSummary sRet = mockSummary(32L, BigDecimal.ZERO, BigDecimal.valueOf(3000));

        LocalDate asOf = LocalDate.of(2026, 10, 31);
        when(journalLineRepository.getAccountBalanceSummaries(1L, asOf))
                .thenReturn(List.of(sAr, sInv, sVatIn, sFix, sVatOut, sAccr, sCap, sRet));
        when(journalLineRepository.getAccountBalanceSummariesBetween(eq(1L), any(), any(), eq(null)))
                .thenReturn(Collections.emptyList());

        BalanceSheetReportDto bs = reportService.getBalanceSheetReport(asOf, null);

        assertEquals(3, bs.currentAssets().size(), "1200, 1300, 1400 must be Current Assets");
        assertEquals(1, bs.nonCurrentAssets().size(), "Only 1500 Fixed Asset is Non-Current Asset");
        assertEquals(2, bs.currentLiabilities().size(), "2200 and 2300 must be Current Liabilities");
        assertTrue(bs.nonCurrentLiabilities().isEmpty());

        // 3200 Retained Earnings must NOT be duplicated in equityLines
        assertEquals(1, bs.equityLines().size());
        assertEquals("3100", bs.equityLines().get(0).accountCode());
        assertEquals(0, BigDecimal.valueOf(3000).compareTo(bs.retainedEarnings()));
        assertEquals(0, BigDecimal.valueOf(7000).compareTo(bs.totalEquity()));
        assertTrue(bs.balanced());

        // Excel export assertions
        byte[] bsXlsx = reportService.exportReport("balance-sheet", "xlsx", Map.of("asOf", "2026-10-31"));
        assertNotNull(bsXlsx);
        try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(bsXlsx))) {
            Sheet sheet = wb.getSheet("Balance Sheet");
            assertNotNull(sheet, "Sheet tab name should be 'Balance Sheet'");
            assertNotNull(sheet.getPaneInformation(), "Freeze pane should be configured");
            assertTrue(sheet.getPaneInformation().isFreezePane());

            int retainedEarningsCount = 0;
            Double retainedEarningsVal = null;
            boolean hasFormulaCell = false;

            for (Row row : sheet) {
                for (int c = 0; c < 2; c++) {
                    Cell labelCell = row.getCell(c);
                    if (labelCell != null && labelCell.getCellType() == CellType.STRING) {
                        String text = labelCell.getStringCellValue();
                        if ("Retained Earnings".equalsIgnoreCase(text.trim())) {
                            retainedEarningsCount++;
                            Cell amountCell = row.getCell(2);
                            if (amountCell != null) {
                                assertEquals(CellType.NUMERIC, amountCell.getCellType(), "Retained earnings amount must be numeric");
                                retainedEarningsVal = amountCell.getNumericCellValue();
                            }
                        }
                    }
                }
                for (Cell cell : row) {
                    if (cell.getCellType() == CellType.FORMULA) {
                        hasFormulaCell = true;
                    }
                }
            }

            assertEquals(1, retainedEarningsCount, "Retained Earnings should appear exactly once in the Excel sheet");
            assertNotNull(retainedEarningsVal);
            assertEquals(3000.0, retainedEarningsVal, 0.001);
            assertTrue(hasFormulaCell, "Sheet must contain live Excel formulas for totals/subtotals");
        }
    }

    @Test
    @DisplayName("Stock Movement Ledger: A SALE movement reduces runningBalance and populates qtyOut cleanly")
    void testStockMovementSaleReducesRunningBalanceAndPopulatesQtyOut() {
        Item item = new Item();
        item.setId(10L);
        item.setCompanyId(1L);
        item.setSku("SKU-100");
        item.setName("Test Product");
        when(itemRepository.findByCompanyIdAndId(1L, 10L)).thenReturn(Optional.of(item));

        Warehouse wh = new Warehouse();
        wh.setId(1L);
        wh.setName("Central Warehouse");
        when(warehouseRepository.findByCompanyId(1L)).thenReturn(List.of(wh));

        // Prior movements (e.g. before 2026-01-01) -> none
        when(stockMovementRepository.findMovements(eq(1L), eq(10L), eq(null), eq(null), any(LocalDate.class)))
                .thenReturn(Collections.emptyList());

        // Opening movement: +80.00
        StockMovement m1 = new StockMovement();
        m1.setId(101L);
        m1.setItemId(10L);
        m1.setWarehouseId(1L);
        m1.setMovementDate(LocalDate.of(2026, 1, 1));
        m1.setMovementType(MovementType.OPENING);
        m1.setQuantity(BigDecimal.valueOf(80));

        // Outbound SALE movement: stored as negative in DB (-1.00)
        StockMovement m2 = new StockMovement();
        m2.setId(102L);
        m2.setItemId(10L);
        m2.setWarehouseId(1L);
        m2.setMovementDate(LocalDate.of(2026, 1, 5));
        m2.setMovementType(MovementType.SALE);
        m2.setQuantity(BigDecimal.valueOf(-1));

        when(stockMovementRepository.findMovements(eq(1L), eq(10L), eq(null), eq(LocalDate.of(2026, 1, 1)), eq(LocalDate.of(2026, 1, 31))))
                .thenReturn(List.of(m1, m2));

        StockMovementReportDto report = reportService.getStockMovementReport(
                10L, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31));

        assertNotNull(report);
        assertEquals(2, report.movements().size());

        StockMovementLineDto openLine = report.movements().get(0);
        assertEquals(0, BigDecimal.valueOf(80).compareTo(openLine.qtyIn()));
        assertEquals(0, BigDecimal.ZERO.compareTo(openLine.qtyOut()));
        assertEquals(0, BigDecimal.valueOf(80).compareTo(openLine.runningBalance()));

        StockMovementLineDto saleLine = report.movements().get(1);
        assertEquals(0, BigDecimal.ZERO.compareTo(saleLine.qtyIn()));
        // qtyOut is cleanly populated as positive magnitude (1.00)
        assertEquals(0, BigDecimal.valueOf(1).compareTo(saleLine.qtyOut()));
        // SALE movement reduces running balance from 80.00 to 79.00
        assertEquals(0, BigDecimal.valueOf(79).compareTo(saleLine.runningBalance()));

        // Closing qty matches final running balance
        assertEquals(0, BigDecimal.valueOf(79).compareTo(report.closingQty()));
    }

    @Test
    @DisplayName("General Ledger: A CREDIT-normal GL account's netMovement equals closingBalance - openingBalance")
    void testGeneralLedgerCreditNormalAccountNetMovement() {
        Account ap = new Account(1L, "2000", "Accounts Payable", AccountType.LIABILITY, "CURRENT_LIABILITY", null, false, NormalBalance.CREDIT);
        ap.setId(201L);
        ap.setOpeningBalance(BigDecimal.ZERO);

        when(accountRepository.findByCompanyIdAndId(1L, 201L)).thenReturn(Optional.of(ap));
        when(journalLineRepository.sumBaseDebitBeforeDate(eq(1L), eq(201L), any(LocalDate.class))).thenReturn(BigDecimal.ZERO);
        when(journalLineRepository.sumBaseCreditBeforeDate(eq(1L), eq(201L), any(LocalDate.class))).thenReturn(BigDecimal.ZERO);

        JournalEntry je = new JournalEntry();
        je.setEntryNo("JE-2026-0001");
        je.setEntryDate(LocalDate.of(2026, 1, 15));
        je.setNarration("Vendor bill posted");

        JournalLine jl = new JournalLine();
        jl.setJournalEntry(je);
        jl.setBaseDebit(BigDecimal.ZERO);
        jl.setBaseCredit(BigDecimal.valueOf(4000));

        when(journalLineRepository.findPostedLinesForAccount(eq(1L), eq(201L), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of(jl));

        GeneralLedgerReportDto glReport = reportService.getGeneralLedgerReport(
                201L, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31));

        assertNotNull(glReport);
        assertEquals(0, BigDecimal.ZERO.compareTo(glReport.openingBalance()));
        assertEquals(0, BigDecimal.valueOf(4000).compareTo(glReport.totalCredit()));
        assertEquals(0, BigDecimal.ZERO.compareTo(glReport.totalDebit()));
        assertEquals(0, BigDecimal.valueOf(4000).compareTo(glReport.closingBalance()));

        // netMovement for CREDIT-normal account must be +4000.00 (closingBalance - openingBalance), NOT -4000.00
        assertEquals(0, BigDecimal.valueOf(4000).compareTo(glReport.netMovement()));
        assertEquals(0, glReport.closingBalance().subtract(glReport.openingBalance()).compareTo(glReport.netMovement()));
        assertEquals(0, glReport.openingBalance().add(glReport.netMovement()).compareTo(glReport.closingBalance()));
    }

    private JournalLineRepository.AccountBalanceSummary mockSummary(Long accId, BigDecimal dr, BigDecimal cr) {
        JournalLineRepository.AccountBalanceSummary s = mock(JournalLineRepository.AccountBalanceSummary.class);
        when(s.getAccountId()).thenReturn(accId);
        when(s.getTotalDebit()).thenReturn(dr);
        when(s.getTotalCredit()).thenReturn(cr);
        return s;
    }
}
