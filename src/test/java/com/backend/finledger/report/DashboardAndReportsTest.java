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
import com.backend.finledger.inventory.entity.Item;
import com.backend.finledger.inventory.repository.*;
import com.backend.finledger.ledger.dto.GeneralLedgerReportDto;
import com.backend.finledger.ledger.dto.TrialBalanceReportDto;
import com.backend.finledger.ledger.entity.Account;
import com.backend.finledger.ledger.entity.AccountType;
import com.backend.finledger.ledger.entity.NormalBalance;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
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

        when(journalLineRepository.getAccountBalanceSummariesBetween(eq(1L), any(LocalDate.class), any(LocalDate.class), eq(null)))
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
        when(stockBalanceRepository.findAll()).thenReturn(Collections.emptyList());

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
    void testGetProfitLossReport() {
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

        when(journalLineRepository.getAccountBalanceSummariesBetween(eq(1L), any(), any(), eq(null)))
                .thenReturn(List.of(s1, s2, s3));

        ProfitLossReportDto pnl = reportService.getProfitLossReport(
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31), null, null);

        assertNotNull(pnl);
        assertEquals(BigDecimal.valueOf(100000), pnl.totalRevenue());
        assertEquals(BigDecimal.valueOf(40000), pnl.totalCogs());
        assertEquals(BigDecimal.valueOf(60000), pnl.grossProfit());
        assertEquals(BigDecimal.valueOf(25000), pnl.totalExpenses());
        assertEquals(BigDecimal.valueOf(35000), pnl.netProfit());
    }
}
