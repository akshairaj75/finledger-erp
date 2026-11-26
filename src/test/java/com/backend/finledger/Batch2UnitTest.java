package com.backend.finledger;

import com.backend.finledger.attachment.dto.AttachmentDto;
import com.backend.finledger.attachment.entity.Attachment;
import com.backend.finledger.attachment.repository.AttachmentRepository;
import com.backend.finledger.attachment.service.FileStorageService;
import com.backend.finledger.attachment.service.impl.AttachmentServiceImpl;
import com.backend.finledger.banking.dto.AutoMatchResultDto;
import com.backend.finledger.banking.entity.BankAccount;
import com.backend.finledger.banking.entity.BankReconciliation;
import com.backend.finledger.banking.entity.BankStatement;
import com.backend.finledger.banking.entity.BankStatementLine;
import com.backend.finledger.banking.entity.MatchStatus;
import com.backend.finledger.banking.entity.ReconciliationStatus;
import com.backend.finledger.banking.repository.BankAccountRepository;
import com.backend.finledger.banking.repository.BankReconciliationRepository;
import com.backend.finledger.banking.repository.BankStatementLineRepository;
import com.backend.finledger.banking.repository.BankStatementRepository;
import com.backend.finledger.banking.service.impl.BankReconciliationServiceImpl;
import com.backend.finledger.common.BusinessException;
import com.backend.finledger.common.ForbiddenException;
import com.backend.finledger.common.NumberSeriesService;
import com.backend.finledger.company.entity.Company;
import com.backend.finledger.company.repository.CompanyRepository;
import com.backend.finledger.crm.entity.Quotation;
import com.backend.finledger.crm.entity.QuotationStatus;
import com.backend.finledger.crm.repository.CrmActivityRepository;
import com.backend.finledger.crm.repository.QuotationRepository;
import com.backend.finledger.expense.entity.*;
import com.backend.finledger.expense.repository.ExpenseCategoryRepository;
import com.backend.finledger.expense.repository.ExpenseRepository;
import com.backend.finledger.inventory.entity.AdjustmentReason;
import com.backend.finledger.inventory.entity.AdjustmentStatus;
import com.backend.finledger.inventory.entity.Item;
import com.backend.finledger.inventory.entity.StockAdjustment;
import com.backend.finledger.inventory.entity.StockAdjustmentLine;
import com.backend.finledger.inventory.repository.StockAdjustmentLineRepository;
import com.backend.finledger.inventory.repository.StockAdjustmentRepository;
import com.backend.finledger.inventory.repository.StockBalanceRepository;
import com.backend.finledger.inventory.repository.WarehouseRepository;
import com.backend.finledger.inventory.repository.ItemRepository;
import com.backend.finledger.inventory.service.CostingService;
import com.backend.finledger.inventory.service.StockService;
import com.backend.finledger.inventory.service.impl.StockAdjustmentServiceImpl;
import com.backend.finledger.ledger.dto.FiscalPeriodDto;
import com.backend.finledger.ledger.dto.SourceDocumentPostingRequest;
import com.backend.finledger.ledger.entity.*;
import com.backend.finledger.ledger.mapper.LedgerMapper;
import com.backend.finledger.ledger.repository.*;
import com.backend.finledger.ledger.service.PostingService;
import com.backend.finledger.ledger.service.impl.FiscalYearPeriodServiceImpl;
import com.backend.finledger.purchases.entity.Bill;
import com.backend.finledger.purchases.entity.BillDocType;
import com.backend.finledger.purchases.entity.BillStatus;
import com.backend.finledger.purchases.repository.BillRepository;
import com.backend.finledger.purchases.service.ApAgeingService;
import com.backend.finledger.report.dto.DashboardSummaryDto;
import com.backend.finledger.report.dto.ExpenseAnalysisReportDto;
import com.backend.finledger.report.dto.VatReturnReportDto;
import com.backend.finledger.report.service.impl.ReportServiceImpl;
import com.backend.finledger.sales.entity.Invoice;
import com.backend.finledger.sales.entity.InvoiceDocType;
import com.backend.finledger.sales.entity.InvoiceStatus;
import com.backend.finledger.sales.repository.InvoiceRepository;
import com.backend.finledger.sales.service.ArAgeingService;
import com.backend.finledger.security.CurrentUser;
import com.backend.finledger.security.UserPrincipal;
import com.backend.finledger.user.entity.User;
import com.backend.finledger.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class Batch2UnitTest {

    private static final Long COMPANY_ID = 100L;
    private static final Long USER_ID = 50L;

    private MockedStatic<CurrentUser> currentUserMock;

    @BeforeEach
    void setUp() {
        currentUserMock = mockStatic(CurrentUser.class);
        currentUserMock.when(CurrentUser::getCompanyId).thenReturn(COMPANY_ID);
        currentUserMock.when(CurrentUser::getUserId).thenReturn(USER_ID);
    }

    @AfterEach
    void tearDown() {
        if (currentUserMock != null) {
            currentUserMock.close();
        }
    }

    // ==========================================
    // 1. Expense Analysis groupBy (category, employee, costCenter)
    // ==========================================
    @Test
    @DisplayName("1. Expense Analysis Report: Group by category, employee, and costCenter")
    void testExpenseAnalysisGroupBy(
            @Mock ExpenseRepository expenseRepo,
            @Mock ExpenseCategoryRepository expCatRepo,
            @Mock CostCenterRepository costCenterRepo,
            @Mock UserRepository userRepo,
            @Mock InvoiceRepository invoiceRepo,
            @Mock BillRepository billRepo,
            @Mock AccountRepository accountRepo,
            @Mock JournalLineRepository jlRepo,
            @Mock JournalEntryRepository jeRepo,
            @Mock BankAccountRepository bankRepo,
            @Mock ArAgeingService arService,
            @Mock ApAgeingService apService) {

        ReportServiceImpl reportService = new ReportServiceImpl(
                accountRepo, jlRepo, jeRepo, bankRepo,
                null, null, null, null, null, null, null,
                expenseRepo, expCatRepo, arService, apService,
                invoiceRepo, billRepo, null, null, null,
                costCenterRepo, userRepo
        );

        // Setup mock categories and cost centers
        ExpenseCategory cat1 = new ExpenseCategory();
        cat1.setId(10L);
        cat1.setName("Travel");

        ExpenseCategory cat2 = new ExpenseCategory();
        cat2.setId(20L);
        cat2.setName("Meals");

        when(expCatRepo.findByCompanyIdOrderByNameAsc(COMPANY_ID)).thenReturn(List.of(cat1, cat2));

        CostCenter cc1 = new CostCenter();
        cc1.setId(301L);
        cc1.setName("Marketing");

        when(costCenterRepo.findByCompanyIdOrderByNameAsc(COMPANY_ID)).thenReturn(List.of(cc1));

        User user = new User();
        user.setId(USER_ID);
        user.setFullName("John Doe");
        when(userRepo.findById(USER_ID)).thenReturn(Optional.of(user));

        // Create sample expense with 2 lines
        Expense exp1 = new Expense();
        exp1.setId(1001L);
        exp1.setCompanyId(COMPANY_ID);
        exp1.setEmployeeUserId(USER_ID);
        exp1.setPaidBy(PaidBy.EMPLOYEE);
        exp1.setExchangeRate(BigDecimal.ONE);
        exp1.setTotal(new BigDecimal("300.00"));

        ExpenseLine line1 = new ExpenseLine();
        line1.setId(1L);
        line1.setExpense(exp1);
        line1.setCategoryId(10L); // Travel
        line1.setCostCenterId(301L); // Marketing
        line1.setAmount(new BigDecimal("100.00"));
        line1.setTaxAmount(new BigDecimal("5.00"));

        ExpenseLine line2 = new ExpenseLine();
        line2.setId(2L);
        line2.setExpense(exp1);
        line2.setCategoryId(20L); // Meals
        line2.setCostCenterId(301L); // Marketing
        line2.setAmount(new BigDecimal("190.00"));
        line2.setTaxAmount(new BigDecimal("5.00"));

        exp1.setLines(List.of(line1, line2));

        when(expenseRepo.searchExpenses(eq(COMPANY_ID), any(), any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(exp1)));

        // Test 1: Category groupBy
        ExpenseAnalysisReportDto catReport = reportService.getExpenseAnalysisReport(null, null, "category");
        assertNotNull(catReport);
        assertEquals(2, catReport.groups().size());
        assertTrue(catReport.groups().stream().anyMatch(g -> g.groupName().equals("Travel") && g.totalAmount().compareTo(new BigDecimal("105.00")) == 0));
        assertTrue(catReport.groups().stream().anyMatch(g -> g.groupName().equals("Meals") && g.totalAmount().compareTo(new BigDecimal("195.00")) == 0));
        assertEquals(new BigDecimal("300.00"), catReport.grandTotal());

        // Test 2: Employee groupBy
        ExpenseAnalysisReportDto empReport = reportService.getExpenseAnalysisReport(null, null, "employee");
        assertNotNull(empReport);
        assertEquals(1, empReport.groups().size());
        assertEquals("John Doe", empReport.groups().get(0).groupName());
        assertEquals(new BigDecimal("300.00"), empReport.groups().get(0).totalAmount());
        assertEquals(new BigDecimal("300.00"), empReport.groups().get(0).reimbursableAmount());

        // Test 3: CostCenter groupBy
        ExpenseAnalysisReportDto ccReport = reportService.getExpenseAnalysisReport(null, null, "costCenter");
        assertNotNull(ccReport);
        assertEquals(1, ccReport.groups().size());
        assertEquals("Marketing", ccReport.groups().get(0).groupName());
        assertEquals(new BigDecimal("300.00"), ccReport.groups().get(0).totalAmount());
    }

    // ==========================================
    // 2. Fiscal Period Lock & Close (with tenant check)
    // ==========================================
    @Test
    @DisplayName("2. Fiscal Period Lock & Close: Company tenant scoping & status transition")
    void testFiscalPeriodLockAndClose(
            @Mock FiscalYearRepository fyRepo,
            @Mock FiscalPeriodRepository fpRepo,
            @Mock LedgerMapper mapper) {

        FiscalYearPeriodServiceImpl fiscalService = new FiscalYearPeriodServiceImpl(fyRepo, fpRepo, mapper);

        FiscalPeriod fp = new FiscalPeriod();
        fp.setId(10L);
        fp.setStatus(FiscalPeriodStatus.OPEN);

        FiscalPeriodDto dto = new FiscalPeriodDto(10L, 1L, (byte) 1, LocalDate.now(), LocalDate.now(), FiscalPeriodStatus.LOCKED);

        when(fpRepo.findByCompanyIdAndId(COMPANY_ID, 10L)).thenReturn(Optional.of(fp));
        when(fpRepo.save(any(FiscalPeriod.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(mapper.toPeriodDto(any())).thenReturn(dto);

        // Lock owned period
        FiscalPeriodDto locked = fiscalService.lockPeriod(10L);
        assertEquals(FiscalPeriodStatus.LOCKED, fp.getStatus());
        assertNotNull(locked);

        // Cross-tenant period lock throws ForbiddenException
        when(fpRepo.findByCompanyIdAndId(COMPANY_ID, 999L)).thenReturn(Optional.empty());
        when(fpRepo.existsById(999L)).thenReturn(true);
        assertThrows(ForbiddenException.class, () -> fiscalService.lockPeriod(999L));

        // Non-existent period lock throws BusinessException
        when(fpRepo.findByCompanyIdAndId(COMPANY_ID, 404L)).thenReturn(Optional.empty());
        when(fpRepo.existsById(404L)).thenReturn(false);
        assertThrows(BusinessException.class, () -> fiscalService.lockPeriod(404L));
    }

    // ==========================================
    // 3. Draft-Only Attachment Deletion Across All Entities
    // ==========================================
    @Test
    @DisplayName("3. Draft-Only Attachment Deletion: Enforce DRAFT parent status across Invoice, Bill, Quotation, Expense")
    void testDraftOnlyAttachmentDeletion(
            @Mock AttachmentRepository attRepo,
            @Mock FileStorageService storageService,
            @Mock ExpenseRepository expRepo,
            @Mock InvoiceRepository invRepo,
            @Mock BillRepository billRepo,
            @Mock QuotationRepository quoteRepo) {

        AttachmentServiceImpl attService = new AttachmentServiceImpl(
                attRepo, storageService, expRepo, invRepo, billRepo, quoteRepo, null, null
        );

        // 1. Invoice attachment
        Attachment invAtt = new Attachment(COMPANY_ID, "INVOICE", 100L, "test.pdf", "application/pdf", 1024L, "key-1", USER_ID);
        invAtt.setId(1L);

        Invoice postedInvoice = new Invoice();
        postedInvoice.setId(100L);
        postedInvoice.setStatus(InvoiceStatus.SENT);

        when(attRepo.findByCompanyIdAndId(COMPANY_ID, 1L)).thenReturn(Optional.of(invAtt));
        when(invRepo.findByCompanyIdAndId(COMPANY_ID, 100L)).thenReturn(Optional.of(postedInvoice));

        // Deleting attachment from posted invoice fails
        BusinessException exInv = assertThrows(BusinessException.class, () -> attService.deleteAttachment(1L));
        assertEquals("ATTACHMENT_LOCKED", exInv.getCode());

        // Now change invoice to DRAFT -> succeeds
        postedInvoice.setStatus(InvoiceStatus.DRAFT);
        assertDoesNotThrow(() -> attService.deleteAttachment(1L));
        verify(storageService).delete("key-1");
        verify(attRepo).delete(invAtt);

        // 2. Bill attachment
        Attachment billAtt = new Attachment(COMPANY_ID, "BILL", 200L, "bill.pdf", "application/pdf", 1024L, "key-2", USER_ID);
        billAtt.setId(2L);
        Bill paidBill = new Bill();
        paidBill.setId(200L);
        paidBill.setStatus(BillStatus.PAID);

        when(attRepo.findByCompanyIdAndId(COMPANY_ID, 2L)).thenReturn(Optional.of(billAtt));
        when(billRepo.findByCompanyIdAndId(COMPANY_ID, 200L)).thenReturn(Optional.of(paidBill));

        BusinessException exBill = assertThrows(BusinessException.class, () -> attService.deleteAttachment(2L));
        assertEquals("ATTACHMENT_LOCKED", exBill.getCode());

        // 3. Quotation attachment
        Attachment quoteAtt = new Attachment(COMPANY_ID, "QUOTATION", 300L, "quote.pdf", "application/pdf", 1024L, "key-3", USER_ID);
        quoteAtt.setId(3L);
        Quotation acceptedQuote = new Quotation();
        acceptedQuote.setId(300L);
        acceptedQuote.setStatus(QuotationStatus.ACCEPTED);

        when(attRepo.findByCompanyIdAndId(COMPANY_ID, 3L)).thenReturn(Optional.of(quoteAtt));
        when(quoteRepo.findByCompanyIdAndId(COMPANY_ID, 300L)).thenReturn(Optional.of(acceptedQuote));

        BusinessException exQuote = assertThrows(BusinessException.class, () -> attService.deleteAttachment(3L));
        assertEquals("ATTACHMENT_LOCKED", exQuote.getCode());
    }

    // ==========================================
    // 4. VAT Return Report (Credit Notes, Debit Notes, FX Rate, Posted Expenses)
    // ==========================================
    @Test
    @DisplayName("4. VAT Return: Credit Note reduces Output VAT, Debit Note reduces Input VAT, FX rate converted, posted expenses included")
    void testVatReturnCreditDebitNotesAndExpenses(
            @Mock InvoiceRepository invoiceRepo,
            @Mock BillRepository billRepo,
            @Mock ExpenseRepository expenseRepo) {

        ReportServiceImpl reportService = new ReportServiceImpl(
                null, null, null, null, null, null, null, null,
                null, null, null, expenseRepo, null, null, null,
                invoiceRepo, billRepo
        );

        // Standard Invoice: 1000 subtotal, 50 VAT, rate 1.0
        Invoice inv1 = new Invoice();
        inv1.setDocType(InvoiceDocType.INVOICE);
        inv1.setSubtotal(new BigDecimal("1000.00"));
        inv1.setTaxTotal(new BigDecimal("50.00"));
        inv1.setExchangeRate(BigDecimal.ONE);

        // Credit Note: 200 subtotal, 10 VAT, foreign rate 2.0 -> in base currency: 400 subtotal, 20 VAT
        Invoice inv2 = new Invoice();
        inv2.setDocType(InvoiceDocType.CREDIT_NOTE);
        inv2.setSubtotal(new BigDecimal("200.00"));
        inv2.setTaxTotal(new BigDecimal("10.00"));
        inv2.setExchangeRate(new BigDecimal("2.000000"));

        when(invoiceRepo.findPostedInvoicesBetween(eq(COMPANY_ID), any(), any())).thenReturn(List.of(inv1, inv2));

        // Standard Bill: 600 subtotal, 30 VAT, rate 1.0
        Bill bill1 = new Bill();
        bill1.setDocType(BillDocType.BILL);
        bill1.setSubtotal(new BigDecimal("600.00"));
        bill1.setTaxTotal(new BigDecimal("30.00"));
        bill1.setExchangeRate(BigDecimal.ONE);

        // Debit Note: 100 subtotal, 5 VAT, rate 1.0
        Bill bill2 = new Bill();
        bill2.setDocType(BillDocType.DEBIT_NOTE);
        bill2.setSubtotal(new BigDecimal("100.00"));
        bill2.setTaxTotal(new BigDecimal("5.00"));
        bill2.setExchangeRate(BigDecimal.ONE);

        when(billRepo.findPostedBillsBetween(eq(COMPANY_ID), any(), any())).thenReturn(List.of(bill1, bill2));

        // Posted Expense: 200 subtotal, 10 VAT, rate 1.0
        Expense exp = new Expense();
        exp.setStatus(ExpenseStatus.POSTED);
        exp.setSubtotal(new BigDecimal("200.00"));
        exp.setTaxTotal(new BigDecimal("10.00"));
        exp.setExchangeRate(BigDecimal.ONE);

        when(expenseRepo.searchExpenses(eq(COMPANY_ID), any(), any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(exp)));

        VatReturnReportDto vat = reportService.getVatReturnReport(LocalDate.now().minusMonths(1), LocalDate.now());
        assertNotNull(vat);

        // Sales: 1000 - 400 = 600. Output VAT: 50 - 20 = 30
        assertEquals(new BigDecimal("600.00"), vat.standardRatedSalesAmount());
        assertEquals(new BigDecimal("30.00"), vat.standardRatedSalesVat());
        assertEquals(new BigDecimal("30.00"), vat.totalOutputVat());

        // Purchases: 600 (bill) - 100 (debit note) + 200 (expense) = 700. Input VAT: 30 - 5 + 10 = 35
        assertEquals(new BigDecimal("700.00"), vat.standardRatedPurchasesAmount());
        assertEquals(new BigDecimal("35.00"), vat.standardRatedPurchasesVat());
        assertEquals(new BigDecimal("35.00"), vat.totalInputVat());

        // Net VAT Due: Output VAT (30) - Input VAT (35) = -5 (refund)
        assertEquals(new BigDecimal("-5.00"), vat.netVatDue());
    }

    // ==========================================
    // 5. Bank Reconciliation autoMatch with Transaction Currency
    // ==========================================
    @Test
    @DisplayName("5. Bank Reconciliation autoMatch: Compare transaction currency jl.getDebit()/jl.getCredit()")
    void testBankReconAutoMatchTransactionCurrency(
            @Mock BankAccountRepository bankAccountRepo,
            @Mock BankStatementRepository statementRepo,
            @Mock BankStatementLineRepository statementLineRepo,
            @Mock BankReconciliationRepository reconciliationRepo,
            @Mock JournalLineRepository journalLineRepo,
            @Mock AccountRepository accountRepo) {

        BankReconciliationServiceImpl reconService = new BankReconciliationServiceImpl(
                reconciliationRepo, bankAccountRepo, statementRepo, statementLineRepo, journalLineRepo, accountRepo, null
        );

        BankAccount usdAccount = new BankAccount();
        usdAccount.setId(5L);
        usdAccount.setCompanyId(COMPANY_ID);
        usdAccount.setCurrencyCode("USD");
        usdAccount.setGlAccountId(1005L);

        when(bankAccountRepo.findByCompanyIdAndId(COMPANY_ID, 5L)).thenReturn(Optional.of(usdAccount));

        BankReconciliation recon = new BankReconciliation();
        recon.setId(50L);
        recon.setBankAccountId(5L);
        recon.setStatementDate(LocalDate.now());
        recon.setStatus(ReconciliationStatus.IN_PROGRESS);

        when(reconciliationRepo.findByCompanyIdAndId(COMPANY_ID, 50L)).thenReturn(Optional.of(recon));

        // Statement line in USD: +1000.00 USD
        BankStatement stmt = new BankStatement();
        stmt.setId(1L);
        stmt.setBankAccountId(5L);

        BankStatementLine stmtLine = new BankStatementLine();
        stmtLine.setId(101L);
        stmtLine.setBankStatement(stmt);
        stmtLine.setTxnDate(LocalDate.now());
        stmtLine.setAmount(new BigDecimal("1000.00")); // USD
        stmtLine.setMatchStatus(MatchStatus.UNMATCHED);

        stmt.setLines(List.of(stmtLine));

        when(statementRepo.findByBankAccountIdOrderByStatementDateDesc(5L)).thenReturn(List.of(stmt));

        // Journal line for USD receipt: debit = 1000.00 USD, baseDebit = 3672.50 AED
        JournalEntry je = new JournalEntry();
        je.setEntryDate(LocalDate.now());

        JournalLine jl = new JournalLine();
        jl.setId(501L);
        jl.setJournalEntry(je);
        jl.setAccountId(1005L);
        jl.setDebit(new BigDecimal("1000.00")); // Txn currency USD
        jl.setCredit(BigDecimal.ZERO);
        jl.setBaseDebit(new BigDecimal("3672.50")); // Base currency AED
        jl.setBaseCredit(BigDecimal.ZERO);
        jl.setIsReconciled(false);

        Account glAcc = new Account();
        glAcc.setId(1005L);
        glAcc.setNormalBalance(NormalBalance.DEBIT);
        glAcc.setOpeningBalance(BigDecimal.ZERO);
        when(accountRepo.findByCompanyIdAndId(COMPANY_ID, 1005L)).thenReturn(Optional.of(glAcc));
        when(journalLineRepo.findAllPostedLinesForAccount(eq(COMPANY_ID), eq(1005L), any())).thenReturn(List.of(jl));
        when(journalLineRepo.findUnreconciledLinesForAccount(eq(COMPANY_ID), eq(1005L), any(LocalDate.class)))
                .thenReturn(List.of(jl));

        AutoMatchResultDto result = reconService.autoMatch(50L);
        assertNotNull(result);
        assertEquals(1, result.matchedCount());
        assertEquals(new BigDecimal("1000.00"), result.reconciledTotal());
        assertEquals(MatchStatus.MATCHED, stmtLine.getMatchStatus());
        assertEquals(501L, stmtLine.getMatchedJournalLineId());
        assertTrue(jl.getIsReconciled());
    }

    // ==========================================
    // 6. Stock Adjustment Company Base Currency
    // ==========================================
    @Test
    @DisplayName("6. Stock Adjustment GL Posting: Use company's baseCurrency instead of hardcoded 'AED'")
    void testStockAdjustmentCompanyBaseCurrency(
            @Mock StockAdjustmentRepository adjRepo,
            @Mock StockAdjustmentLineRepository lineRepo,
            @Mock WarehouseRepository whRepo,
            @Mock ItemRepository itemRepo,
            @Mock StockBalanceRepository sbRepo,
            @Mock AccountRepository accRepo,
            @Mock NumberSeriesService numberSeriesService,
            @Mock PostingService postingService,
            @Mock StockService stockService,
            @Mock CostingService costingService,
            @Mock CompanyRepository companyRepo) {

        StockAdjustmentServiceImpl adjService = new StockAdjustmentServiceImpl(
                adjRepo, lineRepo, whRepo, itemRepo, sbRepo, accRepo,
                numberSeriesService, postingService, stockService, costingService, companyRepo
        );

        Company company = new Company();
        company.setId(COMPANY_ID);
        company.setBaseCurrency("SAR"); // Company uses Saudi Riyal

        when(companyRepo.findById(COMPANY_ID)).thenReturn(Optional.of(company));

        StockAdjustment adj = new StockAdjustment();
        adj.setId(10L);
        adj.setCompanyId(COMPANY_ID);
        adj.setAdjustmentNo("ADJ-2026-001");
        adj.setAdjustmentDate(LocalDate.now());
        adj.setWarehouseId(1L);
        adj.setReason(AdjustmentReason.STOCK_COUNT);
        adj.setAdjustmentAccountId(5200L);
        adj.setStatus(AdjustmentStatus.DRAFT);

        StockAdjustmentLine line = new StockAdjustmentLine();
        line.setId(101L);
        line.setItemId(200L);
        line.setSystemQty(new BigDecimal("10.00"));
        line.setCountedQty(new BigDecimal("15.00"));
        line.setDifferenceQty(new BigDecimal("5.00"));
        line.setUnitCost(new BigDecimal("50.00"));
        line.setValueImpact(new BigDecimal("250.00"));

        adj.setLines(List.of(line));

        when(adjRepo.findByCompanyIdAndId(COMPANY_ID, 10L)).thenReturn(Optional.of(adj));
        when(adjRepo.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Item item = new Item();
        item.setId(200L);
        item.setSku("WIDGET-01");
        item.setName("Widget");
        item.setInventoryAccountId(1300L);
        item.setPurchasePrice(new BigDecimal("50.00"));
        when(itemRepo.findByCompanyIdAndId(COMPANY_ID, 200L)).thenReturn(Optional.of(item));

        com.backend.finledger.ledger.dto.JournalEntryDto mockJe = mock(com.backend.finledger.ledger.dto.JournalEntryDto.class);
        when(mockJe.id()).thenReturn(1L);
        when(postingService.postDocument(any(SourceDocumentPostingRequest.class))).thenReturn(mockJe);

        adjService.postAdjustment(10L);

        ArgumentCaptor<SourceDocumentPostingRequest> captor = ArgumentCaptor.forClass(SourceDocumentPostingRequest.class);
        verify(postingService).postDocument(captor.capture());

        SourceDocumentPostingRequest req = captor.getValue();
        assertEquals("SAR", req.currencyCode(), "Stock adjustment must use company's base currency");
    }

    // ==========================================
    // 7. Dashboard Overdue Tasks Owner Scoping
    // ==========================================
    @Test
    @DisplayName("7. Dashboard Overdue Tasks: Scoped to currentUserId for standard users, company-wide for CRM_VIEW_ALL")
    void testDashboardOverdueTasksOwnerScoping(
            @Mock CrmActivityRepository activityRepo,
            @Mock AccountRepository accountRepo,
            @Mock JournalLineRepository jlRepo,
            @Mock BankAccountRepository bankRepo,
            @Mock ArAgeingService arService,
            @Mock ApAgeingService apService,
            @Mock ItemRepository itemRepo,
            @Mock ExpenseRepository expenseRepo,
            @Mock JournalEntryRepository jeRepo,
            @Mock com.backend.finledger.crm.repository.OpportunityRepository oppRepo,
            @Mock com.backend.finledger.crm.repository.CrmPipelineStageRepository stageRepo,
            @Mock StockBalanceRepository sbRepo) {

        ReportServiceImpl reportService = new ReportServiceImpl(
                accountRepo, jlRepo, jeRepo, bankRepo, oppRepo, stageRepo,
                sbRepo, null, itemRepo, null, null, expenseRepo, null,
                arService, apService, null, null, activityRepo
        );

        when(accountRepo.findByCompanyIdOrderByCodeAsc(COMPANY_ID)).thenReturn(List.of());
        when(jlRepo.getAccountBalanceSummariesBetweenExcludingSourceType(eq(COMPANY_ID), any(), any(), any(), any())).thenReturn(List.of());
        when(arService.getArAgeingReport(any())).thenReturn(new com.backend.finledger.sales.dto.ArAgeingReportDto(
                LocalDate.now(), List.of(), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO
        ));
        when(apService.getApAgeingReport(any())).thenReturn(new com.backend.finledger.purchases.dto.ApAgeingReportDto(
                LocalDate.now(), List.of(), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO
        ));
        when(bankRepo.findByCompanyIdAndIsActiveTrue(COMPANY_ID)).thenReturn(List.of());
        when(oppRepo.findByCompanyId(COMPANY_ID)).thenReturn(List.of());
        when(stageRepo.findByCompanyIdOrderBySortOrderAsc(COMPANY_ID)).thenReturn(List.of());
        when(itemRepo.findByCompanyIdAndIsActiveTrue(COMPANY_ID)).thenReturn(List.of());
        when(expenseRepo.countAwaitingApproval(COMPANY_ID)).thenReturn(0L);
        when(expenseRepo.searchExpenses(eq(COMPANY_ID), any(), any(), any(), any(), any(), any())).thenReturn(new PageImpl<>(List.of()));
        when(jeRepo.searchJournalEntries(eq(COMPANY_ID), any(), any(), any(), any(), any())).thenReturn(new PageImpl<>(List.of()));

        // Case A: Standard user without CRM_VIEW_ALL authority
        currentUserMock.when(() -> CurrentUser.hasAnyAuthority("CRM_VIEW_ALL", "ROLE_ADMIN", "ADMIN")).thenReturn(false);
        when(activityRepo.countOverdueActivitiesForUser(eq(COMPANY_ID), eq(USER_ID), any())).thenReturn(3L);

        DashboardSummaryDto standardSummary = reportService.getDashboardSummary(null, null);
        assertEquals(3L, standardSummary.overdueActivitiesCount(), "Standard user must see only their own overdue activities");
        verify(activityRepo, never()).countOverdueActivities(eq(COMPANY_ID), any());

        // Case B: Admin / CRM_VIEW_ALL user
        currentUserMock.when(() -> CurrentUser.hasAnyAuthority("CRM_VIEW_ALL", "ROLE_ADMIN", "ADMIN")).thenReturn(true);
        when(activityRepo.countOverdueActivities(eq(COMPANY_ID), any())).thenReturn(15L);

        DashboardSummaryDto adminSummary = reportService.getDashboardSummary(null, null);
        assertEquals(15L, adminSummary.overdueActivitiesCount(), "Admin user must see company-wide overdue activities");
    }
}
