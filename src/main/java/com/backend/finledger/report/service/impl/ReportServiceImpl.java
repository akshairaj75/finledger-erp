package com.backend.finledger.report.service.impl;

import com.backend.finledger.common.BusinessException;
import com.backend.finledger.ledger.dto.GeneralLedgerLineDto;
import com.backend.finledger.ledger.dto.GeneralLedgerReportDto;
import com.backend.finledger.ledger.dto.TrialBalanceLineDto;
import com.backend.finledger.ledger.dto.TrialBalanceReportDto;
import com.backend.finledger.ledger.entity.Account;
import com.backend.finledger.ledger.entity.JournalEntry;
import com.backend.finledger.ledger.entity.JournalLine;
import com.backend.finledger.ledger.entity.NormalBalance;
import com.backend.finledger.ledger.repository.AccountRepository;
import com.backend.finledger.ledger.repository.JournalLineRepository;
import com.backend.finledger.report.service.ReportService;
import com.backend.finledger.security.CurrentUser;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ReportServiceImpl implements ReportService {

    private final AccountRepository accountRepository;
    private final JournalLineRepository journalLineRepository;

    public ReportServiceImpl(AccountRepository accountRepository, JournalLineRepository journalLineRepository) {
        this.accountRepository = accountRepository;
        this.journalLineRepository = journalLineRepository;
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

        // 1. Calculate opening balance before `fromDate`
        BigDecimal debitsBefore = journalLineRepository.sumBaseDebitBeforeDate(companyId, accountId, fromDate);
        BigDecimal creditsBefore = journalLineRepository.sumBaseCreditBeforeDate(companyId, accountId, fromDate);
        BigDecimal initialOpening = account.getOpeningBalance() != null ? account.getOpeningBalance() : BigDecimal.ZERO;

        BigDecimal openingBalance;
        if (account.getNormalBalance() == NormalBalance.DEBIT) {
            openingBalance = initialOpening.add(debitsBefore).subtract(creditsBefore);
        } else {
            openingBalance = initialOpening.add(creditsBefore).subtract(debitsBefore);
        }

        // 2. Fetch movements in date range
        List<JournalLine> lines = journalLineRepository.findPostedLinesForAccount(companyId, accountId, fromDate, toDate);

        List<GeneralLedgerLineDto> lineDtos = new ArrayList<>();
        BigDecimal runningBalance = openingBalance;
        BigDecimal periodTotalDebit = BigDecimal.ZERO;
        BigDecimal periodTotalCredit = BigDecimal.ZERO;

        for (JournalLine jl : lines) {
            BigDecimal debit = jl.getBaseDebit() != null ? jl.getBaseDebit() : BigDecimal.ZERO;
            BigDecimal credit = jl.getBaseCredit() != null ? jl.getBaseCredit() : BigDecimal.ZERO;

            periodTotalDebit = periodTotalDebit.add(debit);
            periodTotalCredit = periodTotalCredit.add(credit);

            if (account.getNormalBalance() == NormalBalance.DEBIT) {
                runningBalance = runningBalance.add(debit).subtract(credit);
            } else {
                runningBalance = runningBalance.add(credit).subtract(debit);
            }

            JournalEntry je = jl.getJournalEntry();
            lineDtos.add(new GeneralLedgerLineDto(
                    je.getEntryDate(),
                    je.getEntryNo(),
                    je.getReference(),
                    jl.getDescription() != null ? jl.getDescription() : je.getNarration(),
                    debit,
                    credit,
                    runningBalance
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
                periodTotalDebit,
                periodTotalCredit,
                runningBalance,
                lineDtos
        );
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('REPORT_VIEW') or hasAuthority('LEDGER_VIEW') or isAuthenticated()")
    public TrialBalanceReportDto getTrialBalanceReport(LocalDate asOf) {
        Long companyId = CurrentUser.getCompanyId();
        LocalDate asOfDate = asOf != null ? asOf : LocalDate.now();

        // Retrieve all posting (non-group) accounts
        List<Account> accounts = accountRepository.findByCompanyIdOrderByCodeAsc(companyId).stream()
                .filter(a -> !Boolean.TRUE.equals(a.getIsGroup()))
                .collect(Collectors.toList());

        // Get posted balances up to asOfDate
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

            // Include in report if there are any movements or balances
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
}

