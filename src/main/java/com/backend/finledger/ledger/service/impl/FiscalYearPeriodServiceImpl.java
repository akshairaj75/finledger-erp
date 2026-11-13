package com.backend.finledger.ledger.service.impl;

import com.backend.finledger.common.BusinessException;
import com.backend.finledger.company.entity.Company;
import com.backend.finledger.company.repository.CompanyRepository;
import com.backend.finledger.ledger.dto.CreateFiscalYearRequest;
import com.backend.finledger.ledger.dto.FiscalPeriodDto;
import com.backend.finledger.ledger.dto.FiscalYearDto;
import com.backend.finledger.ledger.dto.JournalLineRequest;
import com.backend.finledger.ledger.dto.SourceDocumentPostingRequest;
import com.backend.finledger.ledger.entity.Account;
import com.backend.finledger.ledger.entity.AccountType;
import com.backend.finledger.ledger.entity.FiscalPeriod;
import com.backend.finledger.ledger.entity.FiscalPeriodStatus;
import com.backend.finledger.ledger.entity.FiscalYear;
import com.backend.finledger.ledger.entity.FiscalYearStatus;
import com.backend.finledger.ledger.entity.JournalSourceType;
import com.backend.finledger.ledger.mapper.LedgerMapper;
import com.backend.finledger.ledger.repository.AccountRepository;
import com.backend.finledger.ledger.repository.FiscalPeriodRepository;
import com.backend.finledger.ledger.repository.FiscalYearRepository;
import com.backend.finledger.ledger.repository.JournalLineRepository;
import com.backend.finledger.ledger.service.FiscalYearPeriodService;
import com.backend.finledger.ledger.service.PostingService;
import com.backend.finledger.security.CurrentUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class FiscalYearPeriodServiceImpl implements FiscalYearPeriodService {

    private final FiscalYearRepository fiscalYearRepository;
    private final FiscalPeriodRepository fiscalPeriodRepository;
    private final LedgerMapper ledgerMapper;
    private final AccountRepository accountRepository;
    private final JournalLineRepository journalLineRepository;
    private final CompanyRepository companyRepository;
    private final PostingService postingService;

    public FiscalYearPeriodServiceImpl(
            FiscalYearRepository fiscalYearRepository,
            FiscalPeriodRepository fiscalPeriodRepository,
            LedgerMapper ledgerMapper) {
        this(fiscalYearRepository, fiscalPeriodRepository, ledgerMapper, null, null, null, null);
    }

    @Autowired
    public FiscalYearPeriodServiceImpl(
            FiscalYearRepository fiscalYearRepository,
            FiscalPeriodRepository fiscalPeriodRepository,
            LedgerMapper ledgerMapper,
            AccountRepository accountRepository,
            JournalLineRepository journalLineRepository,
            CompanyRepository companyRepository,
            PostingService postingService) {
        this.fiscalYearRepository = fiscalYearRepository;
        this.fiscalPeriodRepository = fiscalPeriodRepository;
        this.ledgerMapper = ledgerMapper;
        this.accountRepository = accountRepository;
        this.journalLineRepository = journalLineRepository;
        this.companyRepository = companyRepository;
        this.postingService = postingService;
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyAuthority('SETTINGS_VIEW','SETTINGS_EDIT','ACCOUNT_VIEW','JOURNAL_VIEW','JOURNAL_CREATE')")
    public List<FiscalYearDto> getFiscalYears() {
        Long companyId = CurrentUser.getCompanyId();
        List<FiscalYear> years = fiscalYearRepository.findByCompanyIdOrderByStartDateDesc(companyId);
        return ledgerMapper.toYearDtoList(years);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('SETTINGS_EDIT')")
    public FiscalYearDto createFiscalYear(CreateFiscalYearRequest request) {
        Long companyId = CurrentUser.getCompanyId();

        if (fiscalYearRepository.findByCompanyIdAndName(companyId, request.name()).isPresent()) {
            throw new BusinessException("FISCAL_YEAR_EXISTS", "Fiscal year with name already exists: " + request.name());
        }

        if (request.endDate().isBefore(request.startDate())) {
            throw new BusinessException("INVALID_DATE_RANGE", "End date cannot be before start date");
        }

        FiscalYear fy = new FiscalYear(companyId, request.name(), request.startDate(), request.endDate());
        fy = fiscalYearRepository.save(fy);

        boolean autoGen = request.autoGeneratePeriods() == null || request.autoGeneratePeriods();
        if (autoGen) {
            LocalDate current = request.startDate();
            byte periodNo = 1;
            while (!current.isAfter(request.endDate()) && periodNo <= 12) {
                LocalDate periodEnd = current.withDayOfMonth(current.lengthOfMonth());
                if (periodEnd.isAfter(request.endDate())) {
                    periodEnd = request.endDate();
                }

                FiscalPeriod period = new FiscalPeriod(fy.getId(), periodNo, current, periodEnd);
                fy.getPeriods().add(fiscalPeriodRepository.save(period));

                current = periodEnd.plusDays(1);
                periodNo++;
            }
        }

        return ledgerMapper.toYearDto(fy);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('SETTINGS_EDIT') or hasAuthority('ACCOUNT_EDIT')")
    public FiscalPeriodDto closePeriod(Long periodId) {
        Long companyId = CurrentUser.getCompanyId();

        FiscalPeriod period = fiscalPeriodRepository.findByCompanyIdAndId(companyId, periodId)
                .orElseThrow(() -> new BusinessException("PERIOD_NOT_FOUND", "Fiscal period not found with id: " + periodId));

        period.setStatus(FiscalPeriodStatus.CLOSED);
        FiscalPeriod saved = fiscalPeriodRepository.save(period);
        return ledgerMapper.toPeriodDto(saved);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('SETTINGS_EDIT')")
    public FiscalPeriodDto lockPeriod(Long periodId) {
        Long companyId = CurrentUser.getCompanyId();

        FiscalPeriod period = fiscalPeriodRepository.findByCompanyIdAndId(companyId, periodId)
                .orElseThrow(() -> new BusinessException("PERIOD_NOT_FOUND", "Fiscal period not found with id: " + periodId));

        period.setStatus(FiscalPeriodStatus.LOCKED);
        FiscalPeriod saved = fiscalPeriodRepository.save(period);
        return ledgerMapper.toPeriodDto(saved);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('SETTINGS_EDIT')")
    public FiscalYearDto closeFiscalYear(Long fiscalYearId, Long retainedEarningsAccountId) {
        Long companyId = CurrentUser.getCompanyId();
        FiscalYear fy = fiscalYearRepository.findByCompanyIdAndId(companyId, fiscalYearId)
                .orElseThrow(() -> new BusinessException("FISCAL_YEAR_NOT_FOUND", "Fiscal year not found with id: " + fiscalYearId));

        if (fy.getStatus() == FiscalYearStatus.CLOSED) {
            throw new BusinessException("FISCAL_YEAR_ALREADY_CLOSED", "Fiscal year " + fy.getName() + " is already closed.");
        }

        if (accountRepository != null && journalLineRepository != null && postingService != null) {
            List<Account> allAccounts = accountRepository.findByCompanyIdOrderByCodeAsc(companyId);
            Map<Long, Account> accountMap = allAccounts.stream()
                    .collect(Collectors.toMap(Account::getId, a -> a));

            List<JournalLineRepository.AccountBalanceSummary> summaries =
                    journalLineRepository.getAccountBalanceSummariesBetween(companyId, fy.getStartDate(), fy.getEndDate(), null);

            List<JournalLineRequest> closingLines = new ArrayList<>();
            BigDecimal totalIncomeNet = BigDecimal.ZERO;
            BigDecimal totalExpenseNet = BigDecimal.ZERO;

            for (JournalLineRepository.AccountBalanceSummary sum : summaries) {
                Account acc = accountMap.get(sum.getAccountId());
                if (acc == null || Boolean.TRUE.equals(acc.getIsGroup())) {
                    continue;
                }
                BigDecimal debit = sum.getTotalDebit() != null ? sum.getTotalDebit() : BigDecimal.ZERO;
                BigDecimal credit = sum.getTotalCredit() != null ? sum.getTotalCredit() : BigDecimal.ZERO;

                if (acc.getAccountType() == AccountType.INCOME) {
                    BigDecimal netCredit = credit.subtract(debit).setScale(2, RoundingMode.HALF_UP);
                    if (netCredit.compareTo(BigDecimal.ZERO) > 0) {
                        closingLines.add(new JournalLineRequest(acc.getId(), null,
                                "Year-end close " + fy.getName() + ": " + acc.getName(), netCredit, BigDecimal.ZERO, null, null));
                        totalIncomeNet = totalIncomeNet.add(netCredit);
                    } else if (netCredit.compareTo(BigDecimal.ZERO) < 0) {
                        BigDecimal abs = netCredit.abs();
                        closingLines.add(new JournalLineRequest(acc.getId(), null,
                                "Year-end close " + fy.getName() + ": " + acc.getName(), BigDecimal.ZERO, abs, null, null));
                        totalIncomeNet = totalIncomeNet.subtract(abs);
                    }
                } else if (acc.getAccountType() == AccountType.EXPENSE) {
                    BigDecimal netDebit = debit.subtract(credit).setScale(2, RoundingMode.HALF_UP);
                    if (netDebit.compareTo(BigDecimal.ZERO) > 0) {
                        closingLines.add(new JournalLineRequest(acc.getId(), null,
                                "Year-end close " + fy.getName() + ": " + acc.getName(), BigDecimal.ZERO, netDebit, null, null));
                        totalExpenseNet = totalExpenseNet.add(netDebit);
                    } else if (netDebit.compareTo(BigDecimal.ZERO) < 0) {
                        BigDecimal abs = netDebit.abs();
                        closingLines.add(new JournalLineRequest(acc.getId(), null,
                                "Year-end close " + fy.getName() + ": " + acc.getName(), abs, BigDecimal.ZERO, null, null));
                        totalExpenseNet = totalExpenseNet.subtract(abs);
                    }
                }
            }

            if (!closingLines.isEmpty()) {
                Account retainedEarnings = resolveRetainedEarningsAccount(companyId, retainedEarningsAccountId, allAccounts);
                BigDecimal netProfit = totalIncomeNet.subtract(totalExpenseNet).setScale(2, RoundingMode.HALF_UP);
                if (netProfit.compareTo(BigDecimal.ZERO) > 0) {
                    closingLines.add(new JournalLineRequest(retainedEarnings.getId(), null,
                            "Net profit transfer to Retained Earnings (" + fy.getName() + ")", BigDecimal.ZERO, netProfit, null, null));
                } else if (netProfit.compareTo(BigDecimal.ZERO) < 0) {
                    closingLines.add(new JournalLineRequest(retainedEarnings.getId(), null,
                            "Net loss transfer to Retained Earnings (" + fy.getName() + ")", netProfit.abs(), BigDecimal.ZERO, null, null));
                }

                if (closingLines.size() >= 2) {
                    String baseCurrency = companyRepository != null
                            ? companyRepository.findById(companyId).map(Company::getBaseCurrency).orElse("AED")
                            : "AED";
                    postingService.postDocument(new SourceDocumentPostingRequest(
                            JournalSourceType.CLOSING,
                            fy.getId(),
                            fy.getEndDate(),
                            "YEC-" + fy.getName(),
                            "Year-end closing entry for " + fy.getName(),
                            baseCurrency,
                            BigDecimal.ONE,
                            closingLines
                    ));
                }
            }
        }

        List<FiscalPeriod> periods = fiscalPeriodRepository.findByFiscalYearIdOrderByPeriodNoAsc(fy.getId());
        for (FiscalPeriod period : periods) {
            if (period.getStatus() == FiscalPeriodStatus.OPEN) {
                period.setStatus(FiscalPeriodStatus.CLOSED);
                fiscalPeriodRepository.save(period);
            }
        }
        fy.setStatus(FiscalYearStatus.CLOSED);
        FiscalYear savedFy = fiscalYearRepository.save(fy);
        return ledgerMapper.toYearDto(savedFy);
    }

    private Account resolveRetainedEarningsAccount(Long companyId, Long explicitId, List<Account> allAccounts) {
        if (explicitId != null) {
            Account acc = accountRepository.findByCompanyIdAndId(companyId, explicitId)
                    .orElseThrow(() -> new BusinessException("ACCOUNT_NOT_FOUND", "Retained earnings account not found: " + explicitId));
            if (acc.getAccountType() != AccountType.EQUITY || Boolean.TRUE.equals(acc.getIsGroup()) || Boolean.FALSE.equals(acc.getIsActive())) {
                throw new BusinessException("INVALID_RETAINED_EARNINGS_ACCOUNT", "Selected account must be an active non-group EQUITY account.");
            }
            return acc;
        }
        return allAccounts.stream()
                .filter(a -> a.getAccountType() == AccountType.EQUITY && !Boolean.TRUE.equals(a.getIsGroup()) && Boolean.TRUE.equals(a.getIsActive()))
                .filter(a -> "3200".equals(a.getCode()) || "3100".equals(a.getCode())
                        || (a.getName() != null && a.getName().toLowerCase().contains("retained")))
                .findFirst()
                .orElseGet(() -> allAccounts.stream()
                        .filter(a -> a.getAccountType() == AccountType.EQUITY && !Boolean.TRUE.equals(a.getIsGroup()) && Boolean.TRUE.equals(a.getIsActive()))
                        .findFirst()
                        .orElseThrow(() -> new BusinessException("RETAINED_EARNINGS_NOT_FOUND",
                                "No active non-group EQUITY account configured for year-end closing.")));
    }
}

