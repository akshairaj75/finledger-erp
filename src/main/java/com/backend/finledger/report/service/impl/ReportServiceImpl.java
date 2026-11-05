package com.backend.finledger.report.service.impl;

import com.backend.finledger.banking.entity.BankAccount;
import com.backend.finledger.banking.repository.BankAccountRepository;
import com.backend.finledger.common.BusinessException;
import com.backend.finledger.crm.entity.CrmPipelineStage;
import com.backend.finledger.crm.entity.Opportunity;
import com.backend.finledger.crm.entity.StageType;
import com.backend.finledger.crm.repository.CrmPipelineStageRepository;
import com.backend.finledger.crm.repository.OpportunityRepository;
import com.backend.finledger.expense.entity.Expense;
import com.backend.finledger.expense.entity.ExpenseCategory;
import com.backend.finledger.expense.entity.ExpenseStatus;
import com.backend.finledger.expense.repository.ExpenseCategoryRepository;
import com.backend.finledger.expense.repository.ExpenseRepository;
import com.backend.finledger.inventory.entity.Item;
import com.backend.finledger.inventory.entity.ItemCategory;
import com.backend.finledger.inventory.entity.MovementType;
import com.backend.finledger.inventory.entity.StockBalance;
import com.backend.finledger.inventory.entity.StockMovement;
import com.backend.finledger.inventory.entity.Warehouse;
import com.backend.finledger.inventory.repository.*;
import com.backend.finledger.ledger.dto.GeneralLedgerLineDto;
import com.backend.finledger.ledger.dto.GeneralLedgerReportDto;
import com.backend.finledger.ledger.dto.TrialBalanceLineDto;
import com.backend.finledger.ledger.dto.TrialBalanceReportDto;
import com.backend.finledger.ledger.entity.*;
import com.backend.finledger.ledger.repository.AccountRepository;
import com.backend.finledger.ledger.repository.JournalEntryRepository;
import com.backend.finledger.ledger.repository.JournalLineRepository;
import com.backend.finledger.purchases.dto.ApAgeingReportDto;
import com.backend.finledger.purchases.entity.Bill;
import com.backend.finledger.purchases.repository.BillRepository;
import com.backend.finledger.purchases.service.ApAgeingService;
import com.backend.finledger.report.dto.*;
import com.backend.finledger.report.service.ReportService;
import com.backend.finledger.sales.dto.ArAgeingReportDto;
import com.backend.finledger.sales.entity.Invoice;
import com.backend.finledger.sales.repository.InvoiceRepository;
import com.backend.finledger.sales.service.ArAgeingService;
import com.backend.finledger.security.CurrentUser;
import com.lowagie.text.Document;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ReportServiceImpl implements ReportService {

    private final AccountRepository accountRepository;
    private final JournalLineRepository journalLineRepository;
    private final JournalEntryRepository journalEntryRepository;
    private final BankAccountRepository bankAccountRepository;
    private final OpportunityRepository opportunityRepository;
    private final CrmPipelineStageRepository stageRepository;
    private final StockBalanceRepository stockBalanceRepository;
    private final StockMovementRepository stockMovementRepository;
    private final ItemRepository itemRepository;
    private final ItemCategoryRepository itemCategoryRepository;
    private final WarehouseRepository warehouseRepository;
    private final ExpenseRepository expenseRepository;
    private final ExpenseCategoryRepository expenseCategoryRepository;
    private final ArAgeingService arAgeingService;
    private final ApAgeingService apAgeingService;
    private final InvoiceRepository invoiceRepository;
    private final BillRepository billRepository;

    public ReportServiceImpl(
            AccountRepository accountRepository,
            JournalLineRepository journalLineRepository,
            JournalEntryRepository journalEntryRepository,
            BankAccountRepository bankAccountRepository,
            OpportunityRepository opportunityRepository,
            CrmPipelineStageRepository stageRepository,
            StockBalanceRepository stockBalanceRepository,
            StockMovementRepository stockMovementRepository,
            ItemRepository itemRepository,
            ItemCategoryRepository itemCategoryRepository,
            WarehouseRepository warehouseRepository,
            ExpenseRepository expenseRepository,
            ExpenseCategoryRepository expenseCategoryRepository,
            ArAgeingService arAgeingService,
            ApAgeingService apAgeingService,
            InvoiceRepository invoiceRepository,
            BillRepository billRepository) {
        this.accountRepository = accountRepository;
        this.journalLineRepository = journalLineRepository;
        this.journalEntryRepository = journalEntryRepository;
        this.bankAccountRepository = bankAccountRepository;
        this.opportunityRepository = opportunityRepository;
        this.stageRepository = stageRepository;
        this.stockBalanceRepository = stockBalanceRepository;
        this.stockMovementRepository = stockMovementRepository;
        this.itemRepository = itemRepository;
        this.itemCategoryRepository = itemCategoryRepository;
        this.warehouseRepository = warehouseRepository;
        this.expenseRepository = expenseRepository;
        this.expenseCategoryRepository = expenseCategoryRepository;
        this.arAgeingService = arAgeingService;
        this.apAgeingService = apAgeingService;
        this.invoiceRepository = invoiceRepository;
        this.billRepository = billRepository;
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('REPORT_VIEW') or hasAuthority('LEDGER_VIEW') or isAuthenticated()")
    public GeneralLedgerReportDto getGeneralLedgerReport(Long accountId, LocalDate from, LocalDate to) {
        Long companyId = CurrentUser.getCompanyId();

        Account account = accountRepository.findByCompanyIdAndId(companyId, accountId)
                .orElseThrow(() -> new BusinessException("ACCOUNT_NOT_FOUND", "Account not found with id: " + accountId));

        LocalDate fromDate = from != null ? from : LocalDate.of(1970, 1, 1);
        LocalDate toDate = to != null ? to : LocalDate.now();

        BigDecimal debitsBefore = journalLineRepository.sumBaseDebitBeforeDate(companyId, accountId, fromDate);
        BigDecimal creditsBefore = journalLineRepository.sumBaseCreditBeforeDate(companyId, accountId, fromDate);
        BigDecimal initialOpening = account.getOpeningBalance() != null ? account.getOpeningBalance() : BigDecimal.ZERO;

        BigDecimal openingBalance;
        if (account.getNormalBalance() == NormalBalance.DEBIT) {
            openingBalance = initialOpening.add(debitsBefore).subtract(creditsBefore);
        } else {
            openingBalance = initialOpening.add(creditsBefore).subtract(debitsBefore);
        }

        List<JournalLine> lines = journalLineRepository.findPostedLinesForAccount(companyId, accountId, fromDate, toDate);

        List<GeneralLedgerLineDto> lineDtos = new ArrayList<>();
        BigDecimal currentRunningBalance = openingBalance;
        BigDecimal totalDebit = BigDecimal.ZERO;
        BigDecimal totalCredit = BigDecimal.ZERO;

        for (JournalLine line : lines) {
            JournalEntry entry = line.getJournalEntry();
            BigDecimal debit = line.getBaseDebit();
            BigDecimal credit = line.getBaseCredit();

            totalDebit = totalDebit.add(debit);
            totalCredit = totalCredit.add(credit);

            if (account.getNormalBalance() == NormalBalance.DEBIT) {
                currentRunningBalance = currentRunningBalance.add(debit).subtract(credit);
            } else {
                currentRunningBalance = currentRunningBalance.add(credit).subtract(debit);
            }

            lineDtos.add(new GeneralLedgerLineDto(
                    entry.getEntryDate(),
                    entry.getEntryNo(),
                    entry.getReference(),
                    line.getDescription() != null ? line.getDescription() : entry.getNarration(),
                    debit,
                    credit,
                    currentRunningBalance
            ));
        }

        return new GeneralLedgerReportDto(
                account.getId(),
                account.getCode(),
                account.getName(),
                account.getAccountType(),
                account.getNormalBalance(),
                fromDate,
                toDate,
                openingBalance,
                totalDebit,
                totalCredit,
                currentRunningBalance,
                lineDtos
        );
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('REPORT_VIEW') or hasAuthority('LEDGER_VIEW') or isAuthenticated()")
    public TrialBalanceReportDto getTrialBalanceReport(LocalDate asOf) {
        Long companyId = CurrentUser.getCompanyId();
        LocalDate asOfDate = asOf != null ? asOf : LocalDate.now();

        List<Account> accounts = accountRepository.findByCompanyIdOrderByCodeAsc(companyId).stream()
                .filter(a -> !Boolean.TRUE.equals(a.getIsGroup()))
                .collect(Collectors.toList());

        List<JournalLineRepository.AccountBalanceSummary> summaries =
                journalLineRepository.getAccountBalanceSummaries(companyId, asOfDate);

        Map<Long, JournalLineRepository.AccountBalanceSummary> summaryMap = summaries.stream()
                .collect(Collectors.toMap(JournalLineRepository.AccountBalanceSummary::getAccountId, s -> s));

        List<TrialBalanceLineDto> lines = new ArrayList<>();
        BigDecimal grandTotalDebit = BigDecimal.ZERO;
        BigDecimal grandTotalCredit = BigDecimal.ZERO;

        for (Account a : accounts) {
            JournalLineRepository.AccountBalanceSummary s = summaryMap.get(a.getId());
            BigDecimal debitMovement = (s != null && s.getTotalDebit() != null) ? s.getTotalDebit() : BigDecimal.ZERO;
            BigDecimal creditMovement = (s != null && s.getTotalCredit() != null) ? s.getTotalCredit() : BigDecimal.ZERO;
            BigDecimal opening = a.getOpeningBalance() != null ? a.getOpeningBalance() : BigDecimal.ZERO;

            BigDecimal debitBalance = BigDecimal.ZERO;
            BigDecimal creditBalance = BigDecimal.ZERO;

            if (a.getNormalBalance() == NormalBalance.DEBIT) {
                BigDecimal net = opening.add(debitMovement).subtract(creditMovement);
                if (net.compareTo(BigDecimal.ZERO) >= 0) {
                    debitBalance = net;
                } else {
                    creditBalance = net.abs();
                }
            } else {
                BigDecimal net = opening.add(creditMovement).subtract(debitMovement);
                if (net.compareTo(BigDecimal.ZERO) >= 0) {
                    creditBalance = net;
                } else {
                    debitBalance = net.abs();
                }
            }

            if (debitBalance.compareTo(BigDecimal.ZERO) != 0 || creditBalance.compareTo(BigDecimal.ZERO) != 0
                    || debitMovement.compareTo(BigDecimal.ZERO) != 0 || creditMovement.compareTo(BigDecimal.ZERO) != 0) {
                lines.add(new TrialBalanceLineDto(
                        a.getId(),
                        a.getCode(),
                        a.getName(),
                        a.getAccountType(),
                        a.getNormalBalance(),
                        debitBalance,
                        creditBalance
                ));

                grandTotalDebit = grandTotalDebit.add(debitBalance);
                grandTotalCredit = grandTotalCredit.add(creditBalance);
            }
        }

        boolean balanced = grandTotalDebit.compareTo(grandTotalCredit) == 0;

        return new TrialBalanceReportDto(
                asOfDate,
                grandTotalDebit,
                grandTotalCredit,
                balanced,
                lines
        );
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('DASHBOARD_VIEW') or hasAuthority('REPORT_VIEW') or isAuthenticated()")
    public DashboardSummaryDto getDashboardSummary(LocalDate from, LocalDate to) {
        Long companyId = CurrentUser.getCompanyId();
        LocalDate toDate = to != null ? to : LocalDate.now();
        LocalDate ytdStart = from != null ? from : LocalDate.of(toDate.getYear(), 1, 1);

        // 1. Financial KPIs (YTD Revenue and Expenses)
        List<Account> accounts = accountRepository.findByCompanyIdOrderByCodeAsc(companyId);
        Map<Long, Account> accountMap = accounts.stream().collect(Collectors.toMap(Account::getId, a -> a));

        List<JournalLineRepository.AccountBalanceSummary> ytdSummaries =
                journalLineRepository.getAccountBalanceSummariesBetween(companyId, ytdStart, toDate, null);

        BigDecimal totalRevenueYtd = BigDecimal.ZERO;
        BigDecimal totalExpensesYtd = BigDecimal.ZERO;

        for (JournalLineRepository.AccountBalanceSummary s : ytdSummaries) {
            Account acc = accountMap.get(s.getAccountId());
            if (acc == null) continue;
            BigDecimal debits = s.getTotalDebit() != null ? s.getTotalDebit() : BigDecimal.ZERO;
            BigDecimal credits = s.getTotalCredit() != null ? s.getTotalCredit() : BigDecimal.ZERO;

            if (acc.getAccountType() == AccountType.INCOME) {
                totalRevenueYtd = totalRevenueYtd.add(credits.subtract(debits));
            } else if (acc.getAccountType() == AccountType.EXPENSE) {
                totalExpensesYtd = totalExpensesYtd.add(debits.subtract(credits));
            }
        }

        // AR and AP Ageing totals
        ArAgeingReportDto arReport = arAgeingService.getArAgeingReport(toDate);
        BigDecimal accountsReceivable = arReport.grandTotal() != null ? arReport.grandTotal() : BigDecimal.ZERO;

        ApAgeingReportDto apReport = apAgeingService.getApAgeingReport(toDate);
        BigDecimal accountsPayable = apReport.grandTotal() != null ? apReport.grandTotal() : BigDecimal.ZERO;

        // Cash & Bank balances
        List<BankAccount> bankAccounts = bankAccountRepository.findByCompanyIdAndIsActiveTrue(companyId);
        List<JournalLineRepository.AccountBalanceSummary> asOfSummaries =
                journalLineRepository.getAccountBalanceSummaries(companyId, toDate);
        Map<Long, JournalLineRepository.AccountBalanceSummary> asOfSummaryMap = asOfSummaries.stream()
                .collect(Collectors.toMap(JournalLineRepository.AccountBalanceSummary::getAccountId, s -> s));

        BigDecimal cashAndBank = BigDecimal.ZERO;
        for (BankAccount b : bankAccounts) {
            Account glAcc = accountMap.get(b.getGlAccountId());
            if (glAcc != null) {
                BigDecimal opening = glAcc.getOpeningBalance() != null ? glAcc.getOpeningBalance() : BigDecimal.ZERO;
                JournalLineRepository.AccountBalanceSummary s = asOfSummaryMap.get(glAcc.getId());
                BigDecimal debits = s != null && s.getTotalDebit() != null ? s.getTotalDebit() : BigDecimal.ZERO;
                BigDecimal credits = s != null && s.getTotalCredit() != null ? s.getTotalCredit() : BigDecimal.ZERO;
                if (glAcc.getNormalBalance() == NormalBalance.DEBIT) {
                    cashAndBank = cashAndBank.add(opening.add(debits).subtract(credits));
                } else {
                    cashAndBank = cashAndBank.add(opening.add(credits).subtract(debits));
                }
            }
        }

        // 2. Operational KPIs
        // CRM Open Pipeline
        List<Opportunity> opportunities = opportunityRepository.findByCompanyId(companyId);
        Map<Long, CrmPipelineStage> stageMap = stageRepository.findByCompanyIdOrderBySortOrderAsc(companyId).stream()
                .collect(Collectors.toMap(CrmPipelineStage::getId, s -> s));

        BigDecimal openPipelineValue = BigDecimal.ZERO;
        long openPipelineCount = 0;
        for (Opportunity opp : opportunities) {
            CrmPipelineStage stage = stageMap.get(opp.getStageId());
            if (stage == null || stage.getStageType() == StageType.OPEN) {
                openPipelineValue = openPipelineValue.add(opp.getExpectedValue() != null ? opp.getExpectedValue() : BigDecimal.ZERO);
                openPipelineCount++;
            }
        }

        // Inventory total value & low stock count
        List<Item> items = itemRepository.findByCompanyIdAndIsActiveTrue(companyId);
        Map<Long, List<StockBalance>> stockMap = stockBalanceRepository.findAll().stream()
                .filter(sb -> items.stream().anyMatch(i -> i.getId().equals(sb.getItemId())))
                .collect(Collectors.groupingBy(StockBalance::getItemId));

        BigDecimal totalInventoryValue = BigDecimal.ZERO;
        long lowStockItemCount = 0;

        for (Item item : items) {
            List<StockBalance> balances = stockMap.getOrDefault(item.getId(), List.of());
            BigDecimal itemTotalQty = BigDecimal.ZERO;
            for (StockBalance sb : balances) {
                BigDecimal val = sb.getStockValue() != null ? sb.getStockValue()
                        : (sb.getQtyOnHand() != null && sb.getAvgCost() != null ? sb.getQtyOnHand().multiply(sb.getAvgCost()) : BigDecimal.ZERO);
                totalInventoryValue = totalInventoryValue.add(val);
                itemTotalQty = itemTotalQty.add(sb.getQtyOnHand() != null ? sb.getQtyOnHand() : BigDecimal.ZERO);
            }
            if (item.getReorderLevel() != null && item.getReorderLevel().compareTo(BigDecimal.ZERO) > 0
                    && itemTotalQty.compareTo(item.getReorderLevel()) <= 0) {
                lowStockItemCount++;
            }
        }

        // Expenses awaiting approval
        long expensesAwaitingApprovalCount = expenseRepository.countAwaitingApproval(companyId);
        List<Expense> awaitingExpenses = expenseRepository.searchExpenses(
                companyId, ExpenseStatus.SUBMITTED, null, null, null, null, PageRequest.of(0, 1000)).getContent();
        BigDecimal expensesAwaitingApprovalAmount = awaitingExpenses.stream()
                .map(e -> e.getTotal() != null ? e.getTotal() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 3. 6-month monthly revenue vs expenses
        List<MonthlyFinancialDto> monthlyRevenueExpenses = new ArrayList<>();
        YearMonth currentYm = YearMonth.from(toDate);
        DateTimeFormatter monthFmt = DateTimeFormatter.ofPattern("MMM yyyy");

        for (int i = 5; i >= 0; i--) {
            YearMonth ym = currentYm.minusMonths(i);
            LocalDate mStart = ym.atDay(1);
            LocalDate mEnd = ym.atEndOfMonth();

            List<JournalLineRepository.AccountBalanceSummary> mSummaries =
                    journalLineRepository.getAccountBalanceSummariesBetween(companyId, mStart, mEnd, null);

            BigDecimal mRev = BigDecimal.ZERO;
            BigDecimal mExp = BigDecimal.ZERO;

            for (JournalLineRepository.AccountBalanceSummary s : mSummaries) {
                Account acc = accountMap.get(s.getAccountId());
                if (acc == null) continue;
                BigDecimal d = s.getTotalDebit() != null ? s.getTotalDebit() : BigDecimal.ZERO;
                BigDecimal c = s.getTotalCredit() != null ? s.getTotalCredit() : BigDecimal.ZERO;
                if (acc.getAccountType() == AccountType.INCOME) {
                    mRev = mRev.add(c.subtract(d));
                } else if (acc.getAccountType() == AccountType.EXPENSE) {
                    mExp = mExp.add(d.subtract(c));
                }
            }

            monthlyRevenueExpenses.add(new MonthlyFinancialDto(ym.format(monthFmt), mRev, mExp));
        }

        // 4. Receivables ageing summary
        AgeingSummaryDto receivablesAgeing = new AgeingSummaryDto(
                arReport.totalCurrent() != null ? arReport.totalCurrent() : BigDecimal.ZERO,
                arReport.total1To30() != null ? arReport.total1To30() : BigDecimal.ZERO,
                arReport.total31To60() != null ? arReport.total31To60() : BigDecimal.ZERO,
                arReport.total61To90() != null ? arReport.total61To90() : BigDecimal.ZERO,
                arReport.totalOver90() != null ? arReport.totalOver90() : BigDecimal.ZERO,
                arReport.grandTotal() != null ? arReport.grandTotal() : BigDecimal.ZERO
        );

        // 5. Recent transactions
        List<JournalEntry> recentEntries = journalEntryRepository.searchJournalEntries(
                companyId, null, null, JournalStatus.POSTED, null,
                PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "entryDate", "id"))
        ).getContent();

        List<RecentTransactionDto> recentTransactions = recentEntries.stream()
                .map(je -> new RecentTransactionDto(
                        je.getId(),
                        je.getEntryDate(),
                        je.getSourceType() != null ? je.getSourceType().name() : "MANUAL",
                        je.getEntryNo(),
                        je.getNarration() != null ? je.getNarration() : je.getReference(),
                        je.getTotalDebit(),
                        je.getStatus().name()
                ))
                .collect(Collectors.toList());

        return new DashboardSummaryDto(
                totalRevenueYtd,
                totalExpensesYtd,
                accountsReceivable,
                cashAndBank,
                accountsPayable,
                openPipelineValue,
                openPipelineCount,
                totalInventoryValue,
                lowStockItemCount,
                expensesAwaitingApprovalCount,
                expensesAwaitingApprovalAmount,
                monthlyRevenueExpenses,
                receivablesAgeing,
                recentTransactions
        );
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('REPORT_VIEW') or isAuthenticated()")
    public ProfitLossReportDto getProfitLossReport(LocalDate from, LocalDate to, String compare, Long costCenterId) {
        Long companyId = CurrentUser.getCompanyId();
        LocalDate fromDate = from != null ? from : LocalDate.of(LocalDate.now().getYear(), 1, 1);
        LocalDate toDate = to != null ? to : LocalDate.now();

        List<Account> accounts = accountRepository.findByCompanyIdOrderByCodeAsc(companyId).stream()
                .filter(a -> !Boolean.TRUE.equals(a.getIsGroup()))
                .collect(Collectors.toList());

        List<JournalLineRepository.AccountBalanceSummary> summaries =
                journalLineRepository.getAccountBalanceSummariesBetween(companyId, fromDate, toDate, costCenterId);
        Map<Long, JournalLineRepository.AccountBalanceSummary> summaryMap = summaries.stream()
                .collect(Collectors.toMap(JournalLineRepository.AccountBalanceSummary::getAccountId, s -> s));

        // Comparison period calculation
        Map<Long, JournalLineRepository.AccountBalanceSummary> compareMap = Collections.emptyMap();
        if (compare != null && !compare.isBlank()) {
            LocalDate cFrom;
            LocalDate cTo;
            if ("PRIOR_YEAR".equalsIgnoreCase(compare)) {
                cFrom = fromDate.minusYears(1);
                cTo = toDate.minusYears(1);
            } else { // PRIOR_PERIOD
                long days = ChronoUnit.DAYS.between(fromDate, toDate) + 1;
                cFrom = fromDate.minusDays(days);
                cTo = fromDate.minusDays(1);
            }
            List<JournalLineRepository.AccountBalanceSummary> cSummaries =
                    journalLineRepository.getAccountBalanceSummariesBetween(companyId, cFrom, cTo, costCenterId);
            compareMap = cSummaries.stream()
                    .collect(Collectors.toMap(JournalLineRepository.AccountBalanceSummary::getAccountId, s -> s));
        }

        List<ReportLineDto> revenueLines = new ArrayList<>();
        List<ReportLineDto> cogsLines = new ArrayList<>();
        List<ReportLineDto> expenseLines = new ArrayList<>();

        BigDecimal totalRevenue = BigDecimal.ZERO;
        BigDecimal totalRevenueCompare = BigDecimal.ZERO;
        BigDecimal totalCogs = BigDecimal.ZERO;
        BigDecimal totalCogsCompare = BigDecimal.ZERO;
        BigDecimal totalExpenses = BigDecimal.ZERO;
        BigDecimal totalExpensesCompare = BigDecimal.ZERO;

        for (Account a : accounts) {
            JournalLineRepository.AccountBalanceSummary s = summaryMap.get(a.getId());
            JournalLineRepository.AccountBalanceSummary cs = compareMap.get(a.getId());

            BigDecimal debit = s != null && s.getTotalDebit() != null ? s.getTotalDebit() : BigDecimal.ZERO;
            BigDecimal credit = s != null && s.getTotalCredit() != null ? s.getTotalCredit() : BigDecimal.ZERO;

            BigDecimal cDebit = cs != null && cs.getTotalDebit() != null ? cs.getTotalDebit() : BigDecimal.ZERO;
            BigDecimal cCredit = cs != null && cs.getTotalCredit() != null ? cs.getTotalCredit() : BigDecimal.ZERO;

            if (a.getAccountType() == AccountType.INCOME) {
                BigDecimal net = credit.subtract(debit);
                BigDecimal cNet = cCredit.subtract(cDebit);
                if (net.compareTo(BigDecimal.ZERO) != 0 || cNet.compareTo(BigDecimal.ZERO) != 0) {
                    revenueLines.add(createReportLine(a, net, cNet));
                    totalRevenue = totalRevenue.add(net);
                    totalRevenueCompare = totalRevenueCompare.add(cNet);
                }
            } else if (a.getAccountType() == AccountType.EXPENSE) {
                BigDecimal net = debit.subtract(credit);
                BigDecimal cNet = cDebit.subtract(cCredit);
                boolean isCogs = "COST_OF_SALES".equalsIgnoreCase(a.getSubType())
                        || (a.getCode() != null && a.getCode().startsWith("5"));

                if (isCogs) {
                    if (net.compareTo(BigDecimal.ZERO) != 0 || cNet.compareTo(BigDecimal.ZERO) != 0) {
                        cogsLines.add(createReportLine(a, net, cNet));
                        totalCogs = totalCogs.add(net);
                        totalCogsCompare = totalCogsCompare.add(cNet);
                    }
                } else {
                    if (net.compareTo(BigDecimal.ZERO) != 0 || cNet.compareTo(BigDecimal.ZERO) != 0) {
                        expenseLines.add(createReportLine(a, net, cNet));
                        totalExpenses = totalExpenses.add(net);
                        totalExpensesCompare = totalExpensesCompare.add(cNet);
                    }
                }
            }
        }

        BigDecimal grossProfit = totalRevenue.subtract(totalCogs);
        BigDecimal grossProfitCompare = totalRevenueCompare.subtract(totalCogsCompare);
        BigDecimal netProfit = grossProfit.subtract(totalExpenses);
        BigDecimal netProfitCompare = grossProfitCompare.subtract(totalExpensesCompare);

        return new ProfitLossReportDto(
                fromDate,
                toDate,
                compare,
                revenueLines,
                totalRevenue,
                totalRevenueCompare,
                cogsLines,
                totalCogs,
                totalCogsCompare,
                grossProfit,
                grossProfitCompare,
                expenseLines,
                totalExpenses,
                totalExpensesCompare,
                netProfit,
                netProfitCompare
        );
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('REPORT_VIEW') or isAuthenticated()")
    public BalanceSheetReportDto getBalanceSheetReport(LocalDate asOf, String compare) {
        Long companyId = CurrentUser.getCompanyId();
        LocalDate asOfDate = asOf != null ? asOf : LocalDate.now();

        List<Account> accounts = accountRepository.findByCompanyIdOrderByCodeAsc(companyId).stream()
                .filter(a -> !Boolean.TRUE.equals(a.getIsGroup()))
                .collect(Collectors.toList());

        List<JournalLineRepository.AccountBalanceSummary> summaries =
                journalLineRepository.getAccountBalanceSummaries(companyId, asOfDate);
        Map<Long, JournalLineRepository.AccountBalanceSummary> summaryMap = summaries.stream()
                .collect(Collectors.toMap(JournalLineRepository.AccountBalanceSummary::getAccountId, s -> s));

        List<ReportLineDto> currentAssets = new ArrayList<>();
        List<ReportLineDto> nonCurrentAssets = new ArrayList<>();
        List<ReportLineDto> currentLiabilities = new ArrayList<>();
        List<ReportLineDto> nonCurrentLiabilities = new ArrayList<>();
        List<ReportLineDto> equityLines = new ArrayList<>();

        BigDecimal totalCurrentAssets = BigDecimal.ZERO;
        BigDecimal totalNonCurrentAssets = BigDecimal.ZERO;
        BigDecimal totalCurrentLiabilities = BigDecimal.ZERO;
        BigDecimal totalNonCurrentLiabilities = BigDecimal.ZERO;
        BigDecimal totalEquityAccounts = BigDecimal.ZERO;

        for (Account a : accounts) {
            JournalLineRepository.AccountBalanceSummary s = summaryMap.get(a.getId());
            BigDecimal debit = s != null && s.getTotalDebit() != null ? s.getTotalDebit() : BigDecimal.ZERO;
            BigDecimal credit = s != null && s.getTotalCredit() != null ? s.getTotalCredit() : BigDecimal.ZERO;
            BigDecimal opening = a.getOpeningBalance() != null ? a.getOpeningBalance() : BigDecimal.ZERO;

            if (a.getAccountType() == AccountType.ASSET) {
                BigDecimal bal = opening.add(debit).subtract(credit);
                boolean isCurrent = "CURRENT_ASSET".equalsIgnoreCase(a.getSubType())
                        || (a.getCode() != null && a.getCode().startsWith("11"));
                if (isCurrent) {
                    currentAssets.add(new ReportLineDto(a.getId(), a.getCode(), a.getName(), bal, BigDecimal.ZERO, null));
                    totalCurrentAssets = totalCurrentAssets.add(bal);
                } else {
                    nonCurrentAssets.add(new ReportLineDto(a.getId(), a.getCode(), a.getName(), bal, BigDecimal.ZERO, null));
                    totalNonCurrentAssets = totalNonCurrentAssets.add(bal);
                }
            } else if (a.getAccountType() == AccountType.LIABILITY) {
                BigDecimal bal = opening.add(credit).subtract(debit);
                boolean isCurrent = "CURRENT_LIABILITY".equalsIgnoreCase(a.getSubType())
                        || (a.getCode() != null && a.getCode().startsWith("21"));
                if (isCurrent) {
                    currentLiabilities.add(new ReportLineDto(a.getId(), a.getCode(), a.getName(), bal, BigDecimal.ZERO, null));
                    totalCurrentLiabilities = totalCurrentLiabilities.add(bal);
                } else {
                    nonCurrentLiabilities.add(new ReportLineDto(a.getId(), a.getCode(), a.getName(), bal, BigDecimal.ZERO, null));
                    totalNonCurrentLiabilities = totalNonCurrentLiabilities.add(bal);
                }
            } else if (a.getAccountType() == AccountType.EQUITY) {
                BigDecimal bal = opening.add(credit).subtract(debit);
                equityLines.add(new ReportLineDto(a.getId(), a.getCode(), a.getName(), bal, BigDecimal.ZERO, null));
                totalEquityAccounts = totalEquityAccounts.add(bal);
            }
        }

        // Net income Year to Date for Retained Earnings / Current Period Earnings
        LocalDate yearStart = LocalDate.of(asOfDate.getYear(), 1, 1);
        List<JournalLineRepository.AccountBalanceSummary> ytdSummaries =
                journalLineRepository.getAccountBalanceSummariesBetween(companyId, yearStart, asOfDate, null);
        BigDecimal netIncomeYtd = BigDecimal.ZERO;
        for (JournalLineRepository.AccountBalanceSummary s : ytdSummaries) {
            Account a = accounts.stream().filter(acc -> acc.getId().equals(s.getAccountId())).findFirst().orElse(null);
            if (a == null) continue;
            BigDecimal d = s.getTotalDebit() != null ? s.getTotalDebit() : BigDecimal.ZERO;
            BigDecimal c = s.getTotalCredit() != null ? s.getTotalCredit() : BigDecimal.ZERO;
            if (a.getAccountType() == AccountType.INCOME) {
                netIncomeYtd = netIncomeYtd.add(c.subtract(d));
            } else if (a.getAccountType() == AccountType.EXPENSE) {
                netIncomeYtd = netIncomeYtd.subtract(d.subtract(c));
            }
        }

        BigDecimal totalAssets = totalCurrentAssets.add(totalNonCurrentAssets);
        BigDecimal totalLiabilities = totalCurrentLiabilities.add(totalNonCurrentLiabilities);
        BigDecimal totalEquity = totalEquityAccounts.add(netIncomeYtd);
        BigDecimal totalLiabilitiesAndEquity = totalLiabilities.add(totalEquity);
        boolean balanced = totalAssets.compareTo(totalLiabilitiesAndEquity) == 0;

        return new BalanceSheetReportDto(
                asOfDate,
                compare,
                currentAssets,
                totalCurrentAssets,
                nonCurrentAssets,
                totalNonCurrentAssets,
                totalAssets,
                currentLiabilities,
                totalCurrentLiabilities,
                nonCurrentLiabilities,
                totalNonCurrentLiabilities,
                totalLiabilities,
                equityLines,
                BigDecimal.ZERO,
                netIncomeYtd,
                totalEquity,
                totalLiabilitiesAndEquity,
                balanced
        );
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('REPORT_VIEW') or isAuthenticated()")
    public VatReturnReportDto getVatReturnReport(LocalDate from, LocalDate to) {
        Long companyId = CurrentUser.getCompanyId();
        LocalDate fromDate = from != null ? from : LocalDate.of(LocalDate.now().getYear(), 1, 1);
        LocalDate toDate = to != null ? to : LocalDate.now();

        List<Invoice> invoices = invoiceRepository.findPostedInvoicesBetween(companyId, fromDate, toDate);
        List<Bill> bills = billRepository.findPostedBillsBetween(companyId, fromDate, toDate);

        BigDecimal stdSalesAmount = BigDecimal.ZERO;
        BigDecimal stdSalesVat = BigDecimal.ZERO;
        BigDecimal zeroSalesAmount = BigDecimal.ZERO;
        BigDecimal exemptSalesAmount = BigDecimal.ZERO;

        for (Invoice inv : invoices) {
            if (inv.getTaxTotal() != null && inv.getTaxTotal().compareTo(BigDecimal.ZERO) > 0) {
                stdSalesAmount = stdSalesAmount.add(inv.getSubtotal() != null ? inv.getSubtotal() : BigDecimal.ZERO);
                stdSalesVat = stdSalesVat.add(inv.getTaxTotal());
            } else {
                zeroSalesAmount = zeroSalesAmount.add(inv.getSubtotal() != null ? inv.getSubtotal() : BigDecimal.ZERO);
            }
        }

        BigDecimal stdPurchasesAmount = BigDecimal.ZERO;
        BigDecimal stdPurchasesVat = BigDecimal.ZERO;

        for (Bill b : bills) {
            stdPurchasesAmount = stdPurchasesAmount.add(b.getSubtotal() != null ? b.getSubtotal() : BigDecimal.ZERO);
            stdPurchasesVat = stdPurchasesVat.add(b.getTaxTotal() != null ? b.getTaxTotal() : BigDecimal.ZERO);
        }

        BigDecimal totalOutputVat = stdSalesVat;
        BigDecimal totalInputVat = stdPurchasesVat;
        BigDecimal netVatDue = totalOutputVat.subtract(totalInputVat);

        return new VatReturnReportDto(
                fromDate,
                toDate,
                stdSalesAmount,
                stdSalesVat,
                zeroSalesAmount,
                exemptSalesAmount,
                stdPurchasesAmount,
                stdPurchasesVat,
                totalOutputVat,
                totalInputVat,
                netVatDue
        );
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('REPORT_VIEW') or isAuthenticated()")
    public StockValuationReportDto getStockValuationReport(LocalDate asOf, Long warehouseId) {
        Long companyId = CurrentUser.getCompanyId();
        LocalDate asOfDate = asOf != null ? asOf : LocalDate.now();

        List<Item> items = itemRepository.findByCompanyIdAndIsActiveTrue(companyId);
        Map<Long, ItemCategory> categoryMap = itemCategoryRepository.findByCompanyId(companyId).stream()
                .collect(Collectors.toMap(ItemCategory::getId, c -> c));

        Map<Long, Warehouse> warehouseMap = warehouseRepository.findByCompanyId(companyId).stream()
                .collect(Collectors.toMap(Warehouse::getId, w -> w));

        List<StockValuationLineDto> lines = new ArrayList<>();
        BigDecimal grandTotalValuation = BigDecimal.ZERO;

        for (Item item : items) {
            List<StockBalance> balances;
            if (warehouseId != null) {
                balances = stockBalanceRepository.findByItemIdAndWarehouseId(item.getId(), warehouseId)
                        .map(List::of).orElse(List.of());
            } else {
                balances = stockBalanceRepository.findByItemId(item.getId());
            }

            String catName = item.getCategoryId() != null && categoryMap.containsKey(item.getCategoryId())
                    ? categoryMap.get(item.getCategoryId()).getName() : "General";

            for (StockBalance sb : balances) {
                Warehouse wh = warehouseMap.get(sb.getWarehouseId());
                String whName = wh != null ? wh.getName() : "Main Warehouse";
                BigDecimal qty = sb.getQtyOnHand() != null ? sb.getQtyOnHand() : BigDecimal.ZERO;
                BigDecimal cost = sb.getAvgCost() != null ? sb.getAvgCost() : (item.getPurchasePrice() != null ? item.getPurchasePrice() : BigDecimal.ZERO);
                BigDecimal totalVal = sb.getStockValue() != null ? sb.getStockValue() : qty.multiply(cost);

                lines.add(new StockValuationLineDto(
                        item.getId(),
                        item.getSku(),
                        item.getName(),
                        catName,
                        whName,
                        qty,
                        cost,
                        totalVal
                ));

                grandTotalValuation = grandTotalValuation.add(totalVal);
            }
        }

        String targetWhName = warehouseId != null && warehouseMap.containsKey(warehouseId)
                ? warehouseMap.get(warehouseId).getName() : "All Warehouses";

        return new StockValuationReportDto(
                asOfDate,
                warehouseId,
                targetWhName,
                lines,
                grandTotalValuation,
                lines.size()
        );
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('REPORT_VIEW') or isAuthenticated()")
    public StockMovementReportDto getStockMovementReport(Long itemId, LocalDate from, LocalDate to) {
        Long companyId = CurrentUser.getCompanyId();
        Item item = itemRepository.findByCompanyIdAndId(companyId, itemId)
                .orElseThrow(() -> new BusinessException("ITEM_NOT_FOUND", "Item not found with id: " + itemId));

        LocalDate fromDate = from != null ? from : LocalDate.of(LocalDate.now().getYear(), 1, 1);
        LocalDate toDate = to != null ? to : LocalDate.now();

        List<StockMovement> movements = stockMovementRepository.findMovements(companyId, itemId, null, fromDate, toDate);
        Map<Long, Warehouse> warehouseMap = warehouseRepository.findByCompanyId(companyId).stream()
                .collect(Collectors.toMap(Warehouse::getId, w -> w));

        List<StockMovementLineDto> lineDtos = new ArrayList<>();
        BigDecimal currentBalance = BigDecimal.ZERO;

        for (StockMovement sm : movements) {
            Warehouse wh = warehouseMap.get(sm.getWarehouseId());
            String whName = wh != null ? wh.getName() : "Default";

            BigDecimal qtyIn = BigDecimal.ZERO;
            BigDecimal qtyOut = BigDecimal.ZERO;

            boolean isInward = sm.getMovementType() == MovementType.OPENING
                    || sm.getMovementType() == MovementType.PURCHASE
                    || sm.getMovementType() == MovementType.SALE_RETURN
                    || sm.getMovementType() == MovementType.ADJUSTMENT_IN
                    || sm.getMovementType() == MovementType.TRANSFER_IN;

            if (isInward) {
                qtyIn = sm.getQuantity();
                currentBalance = currentBalance.add(qtyIn);
            } else {
                qtyOut = sm.getQuantity();
                currentBalance = currentBalance.subtract(qtyOut);
            }

            lineDtos.add(new StockMovementLineDto(
                    sm.getId(),
                    sm.getMovementDate(),
                    sm.getMovementType().name(),
                    sm.getSourceType().name() + " #" + sm.getSourceId(),
                    whName,
                    qtyIn,
                    qtyOut,
                    currentBalance,
                    sm.getUnitCost()
            ));
        }

        return new StockMovementReportDto(
                item.getId(),
                item.getSku(),
                item.getName(),
                fromDate,
                toDate,
                BigDecimal.ZERO,
                currentBalance,
                lineDtos
        );
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('REPORT_VIEW') or isAuthenticated()")
    public PipelineForecastReportDto getPipelineForecastReport(LocalDate from, LocalDate to, Long ownerId) {
        Long companyId = CurrentUser.getCompanyId();
        LocalDate fromDate = from != null ? from : LocalDate.of(LocalDate.now().getYear(), 1, 1);
        LocalDate toDate = to != null ? to : LocalDate.now().plusMonths(6);

        List<CrmPipelineStage> stages = stageRepository.findByCompanyIdOrderBySortOrderAsc(companyId);
        List<Opportunity> allOpportunities = opportunityRepository.findByCompanyId(companyId);

        List<PipelineStageSummaryDto> stageSummaries = new ArrayList<>();
        BigDecimal grandTotalValue = BigDecimal.ZERO;
        BigDecimal grandTotalWeighted = BigDecimal.ZERO;
        int grandTotalDeals = 0;

        for (CrmPipelineStage stage : stages) {
            List<Opportunity> stageOpps = allOpportunities.stream()
                    .filter(o -> o.getStageId().equals(stage.getId()))
                    .filter(o -> ownerId == null || (o.getOwnerUserId() != null && o.getOwnerUserId().equals(ownerId)))
                    .collect(Collectors.toList());

            int count = stageOpps.size();
            BigDecimal totalVal = stageOpps.stream()
                    .map(o -> o.getExpectedValue() != null ? o.getExpectedValue() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            int prob = stage.getProbabilityPct() != null ? stage.getProbabilityPct() : 0;
            BigDecimal weightedVal = totalVal.multiply(BigDecimal.valueOf(prob)).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

            stageSummaries.add(new PipelineStageSummaryDto(
                    stage.getId(),
                    stage.getName(),
                    count,
                    totalVal,
                    weightedVal,
                    prob
            ));

            grandTotalValue = grandTotalValue.add(totalVal);
            grandTotalWeighted = grandTotalWeighted.add(weightedVal);
            grandTotalDeals += count;
        }

        return new PipelineForecastReportDto(
                fromDate,
                toDate,
                ownerId,
                stageSummaries,
                grandTotalValue,
                grandTotalWeighted,
                grandTotalDeals
        );
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('REPORT_VIEW') or isAuthenticated()")
    public ExpenseAnalysisReportDto getExpenseAnalysisReport(LocalDate from, LocalDate to, String groupBy) {
        Long companyId = CurrentUser.getCompanyId();
        LocalDate fromDate = from != null ? from : LocalDate.of(LocalDate.now().getYear(), 1, 1);
        LocalDate toDate = to != null ? to : LocalDate.now();

        List<Expense> expenses = expenseRepository.searchExpenses(
                companyId, null, null, null, fromDate, toDate, PageRequest.of(0, 5000)).getContent();

        BigDecimal grandTotal = expenses.stream()
                .map(e -> e.getTotal() != null ? e.getTotal() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, List<Expense>> grouped = new LinkedHashMap<>();
        for (Expense exp : expenses) {
            String key;
            if ("employee".equalsIgnoreCase(groupBy)) {
                key = exp.getEmployeeUserId() != null ? "Employee #" + exp.getEmployeeUserId() : "Unassigned";
            } else { // default: general category breakdown
                key = exp.getPayeeName() != null && !exp.getPayeeName().isBlank() ? exp.getPayeeName() : "General Operations";
            }
            grouped.computeIfAbsent(key, k -> new ArrayList<>()).add(exp);
        }

        List<ExpenseAnalysisGroupDto> groupDtos = new ArrayList<>();
        for (Map.Entry<String, List<Expense>> entry : grouped.entrySet()) {
            List<Expense> groupList = entry.getValue();
            int count = groupList.size();
            BigDecimal totalAmt = groupList.stream()
                    .map(e -> e.getTotal() != null ? e.getTotal() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal reimbursableAmt = groupList.stream()
                    .filter(e -> e.getPaidBy() != null && "EMPLOYEE".equalsIgnoreCase(e.getPaidBy().name()))
                    .map(e -> e.getTotal() != null ? e.getTotal() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            Double pct = grandTotal.compareTo(BigDecimal.ZERO) > 0
                    ? totalAmt.divide(grandTotal, 4, RoundingMode.HALF_UP).doubleValue() * 100
                    : 0.0;

            groupDtos.add(new ExpenseAnalysisGroupDto(
                    entry.getKey(),
                    entry.getKey(),
                    count,
                    totalAmt,
                    reimbursableAmt,
                    pct
            ));
        }

        return new ExpenseAnalysisReportDto(
                fromDate,
                toDate,
                groupBy != null ? groupBy : "category",
                groupDtos,
                grandTotal
        );
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportReport(String type, String format, Map<String, String> params) {
        if ("pdf".equalsIgnoreCase(format)) {
            return exportToPdf(type, params);
        } else {
            return exportToExcel(type, params);
        }
    }

    private byte[] exportToPdf(String type, Map<String, String> params) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4.rotate());
            PdfWriter.getInstance(document, out);
            document.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, Color.DARK_GRAY);
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.WHITE);
            Font cellFont = FontFactory.getFont(FontFactory.HELVETICA, 9, Color.BLACK);

            document.add(new Paragraph("FinLedger Financial Report: " + type.toUpperCase().replace('-', ' '), titleFont));
            document.add(new Paragraph("Generated on: " + LocalDate.now() + " | Tenant Scope: Current Organization\n\n"));

            PdfPTable table = new PdfPTable(4);
            table.setWidthPercentage(100);
            table.setSpacingBefore(10f);

            // Add Header
            String[] headers = {"Account / Code", "Description", "Debit / Budget", "Credit / Amount"};
            for (String h : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(h, headerFont));
                cell.setBackgroundColor(new Color(24, 76, 120));
                cell.setPadding(6f);
                table.addCell(cell);
            }

            // Fill rows depending on report type
            if ("profit-loss".equalsIgnoreCase(type)) {
                ProfitLossReportDto pnl = getProfitLossReport(null, null, null, null);
                for (ReportLineDto line : pnl.revenueLines()) {
                    table.addCell(new Phrase(line.accountCode(), cellFont));
                    table.addCell(new Phrase(line.accountName(), cellFont));
                    table.addCell(new Phrase("-", cellFont));
                    table.addCell(new Phrase(line.amount().toString(), cellFont));
                }
                for (ReportLineDto line : pnl.expenseLines()) {
                    table.addCell(new Phrase(line.accountCode(), cellFont));
                    table.addCell(new Phrase(line.accountName(), cellFont));
                    table.addCell(new Phrase(line.amount().toString(), cellFont));
                    table.addCell(new Phrase("-", cellFont));
                }
            } else if ("balance-sheet".equalsIgnoreCase(type)) {
                BalanceSheetReportDto bs = getBalanceSheetReport(null, null);
                for (ReportLineDto line : bs.currentAssets()) {
                    table.addCell(new Phrase(line.accountCode(), cellFont));
                    table.addCell(new Phrase(line.accountName(), cellFont));
                    table.addCell(new Phrase(line.amount().toString(), cellFont));
                    table.addCell(new Phrase("-", cellFont));
                }
                for (ReportLineDto line : bs.currentLiabilities()) {
                    table.addCell(new Phrase(line.accountCode(), cellFont));
                    table.addCell(new Phrase(line.accountName(), cellFont));
                    table.addCell(new Phrase("-", cellFont));
                    table.addCell(new Phrase(line.amount().toString(), cellFont));
                }
            } else {
                TrialBalanceReportDto tb = getTrialBalanceReport(null);
                for (TrialBalanceLineDto line : tb.lines()) {
                    table.addCell(new Phrase(line.accountCode(), cellFont));
                    table.addCell(new Phrase(line.accountName(), cellFont));
                    table.addCell(new Phrase(line.debitBalance().toString(), cellFont));
                    table.addCell(new Phrase(line.creditBalance().toString(), cellFont));
                }
            }

            document.add(table);
            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new BusinessException("EXPORT_ERROR", "Failed to generate PDF report: " + e.getMessage());
        }
    }

    private byte[] exportToExcel(String type, Map<String, String> params) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Report");

            org.apache.poi.ss.usermodel.Font boldFont = workbook.createFont();
            boldFont.setBold(true);

            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFont(boldFont);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            Row titleRow = sheet.createRow(0);
            org.apache.poi.ss.usermodel.Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("FinLedger - " + type.toUpperCase().replace('-', ' ') + " Report");
            titleCell.setCellStyle(headerStyle);

            Row headerRow = sheet.createRow(2);
            String[] headers = {"Account Code", "Account Name", "Debit / Inflow", "Credit / Outflow"};
            for (int i = 0; i < headers.length; i++) {
                org.apache.poi.ss.usermodel.Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowIdx = 3;
            if ("profit-loss".equalsIgnoreCase(type)) {
                ProfitLossReportDto pnl = getProfitLossReport(null, null, null, null);
                for (ReportLineDto line : pnl.revenueLines()) {
                    Row r = sheet.createRow(rowIdx++);
                    r.createCell(0).setCellValue(line.accountCode());
                    r.createCell(1).setCellValue(line.accountName());
                    r.createCell(2).setCellValue(0.0);
                    r.createCell(3).setCellValue(line.amount().doubleValue());
                }
                for (ReportLineDto line : pnl.expenseLines()) {
                    Row r = sheet.createRow(rowIdx++);
                    r.createCell(0).setCellValue(line.accountCode());
                    r.createCell(1).setCellValue(line.accountName());
                    r.createCell(2).setCellValue(line.amount().doubleValue());
                    r.createCell(3).setCellValue(0.0);
                }
            } else {
                TrialBalanceReportDto tb = getTrialBalanceReport(null);
                for (TrialBalanceLineDto line : tb.lines()) {
                    Row r = sheet.createRow(rowIdx++);
                    r.createCell(0).setCellValue(line.accountCode());
                    r.createCell(1).setCellValue(line.accountName());
                    r.createCell(2).setCellValue(line.debitBalance().doubleValue());
                    r.createCell(3).setCellValue(line.creditBalance().doubleValue());
                }
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new BusinessException("EXPORT_ERROR", "Failed to generate Excel report: " + e.getMessage());
        }
    }

    private ReportLineDto createReportLine(Account a, BigDecimal amount, BigDecimal compareAmount) {
        Double changePct = null;
        if (compareAmount != null && compareAmount.compareTo(BigDecimal.ZERO) != 0) {
            changePct = amount.subtract(compareAmount)
                    .divide(compareAmount.abs(), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .doubleValue();
        }
        return new ReportLineDto(
                a.getId(),
                a.getCode(),
                a.getName(),
                amount,
                compareAmount,
                changePct
        );
    }
}
