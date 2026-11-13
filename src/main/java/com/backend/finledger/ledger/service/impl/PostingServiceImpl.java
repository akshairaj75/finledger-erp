package com.backend.finledger.ledger.service.impl;

import com.backend.finledger.common.BusinessException;
import com.backend.finledger.common.DocType;
import com.backend.finledger.common.NumberSeriesService;
import com.backend.finledger.common.PageResponse;
import com.backend.finledger.company.entity.Company;
import com.backend.finledger.company.repository.CompanyRepository;
import com.backend.finledger.ledger.dto.CreateJournalEntryRequest;
import com.backend.finledger.ledger.dto.JournalEntryDto;
import com.backend.finledger.ledger.dto.JournalLineRequest;
import com.backend.finledger.ledger.dto.SourceDocumentPostingRequest;
import com.backend.finledger.ledger.entity.*;
import com.backend.finledger.ledger.mapper.LedgerMapper;
import com.backend.finledger.ledger.repository.*;
import com.backend.finledger.ledger.service.PostingService;
import com.backend.finledger.security.CurrentUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
public class PostingServiceImpl implements PostingService {

    private final JournalEntryRepository journalEntryRepository;
    private final JournalLineRepository journalLineRepository;
    private final AccountRepository accountRepository;
    private final FiscalPeriodRepository fiscalPeriodRepository;
    private final CostCenterRepository costCenterRepository;
    private final CompanyRepository companyRepository;
    private final NumberSeriesService numberSeriesService;
    private final LedgerMapper ledgerMapper;

    public PostingServiceImpl(
            JournalEntryRepository journalEntryRepository,
            JournalLineRepository journalLineRepository,
            AccountRepository accountRepository,
            FiscalPeriodRepository fiscalPeriodRepository,
            CostCenterRepository costCenterRepository,
            CompanyRepository companyRepository,
            NumberSeriesService numberSeriesService,
            LedgerMapper ledgerMapper) {
        this.journalEntryRepository = journalEntryRepository;
        this.journalLineRepository = journalLineRepository;
        this.accountRepository = accountRepository;
        this.fiscalPeriodRepository = fiscalPeriodRepository;
        this.costCenterRepository = costCenterRepository;
        this.companyRepository = companyRepository;
        this.numberSeriesService = numberSeriesService;
        this.ledgerMapper = ledgerMapper;
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyAuthority('JOURNAL_VIEW','LEDGER_VIEW','REPORT_VIEW','ACCOUNT_VIEW')")
    public PageResponse<JournalEntryDto> getJournalEntries(
            LocalDate from, LocalDate to, JournalStatus status, JournalSourceType sourceType, Pageable pageable) {
        Long companyId = CurrentUser.getCompanyId();
        Page<JournalEntry> page = journalEntryRepository.searchJournalEntries(companyId, from, to, status, sourceType, pageable);
        return PageResponse.from(page.map(ledgerMapper::toEntryDto));
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyAuthority('JOURNAL_VIEW','LEDGER_VIEW','REPORT_VIEW','ACCOUNT_VIEW')")
    public JournalEntryDto getJournalEntryById(Long id) {
        Long companyId = CurrentUser.getCompanyId();
        JournalEntry entry = journalEntryRepository.findByCompanyIdAndId(companyId, id)
                .orElseThrow(() -> new BusinessException("JOURNAL_NOT_FOUND", "Journal entry not found with id: " + id));
        return ledgerMapper.toEntryDto(entry);
    }

    /**
     * Creates a manual journal entry (draft or posted).
     */
    @Override
    @Transactional
    @PreAuthorize("hasAuthority('JOURNAL_CREATE') or hasAuthority('JOURNAL_POST')")
    public JournalEntryDto createJournalEntry(CreateJournalEntryRequest request) {
        Long companyId = CurrentUser.getCompanyId();

        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new BusinessException("COMPANY_NOT_FOUND", "Company not found for id: " + companyId));

        String currencyCode = request.currencyCode() != null ? request.currencyCode() : company.getBaseCurrency();
        BigDecimal exchangeRate = (request.exchangeRate() != null && request.exchangeRate().compareTo(BigDecimal.ZERO) > 0)
                ? request.exchangeRate() : BigDecimal.ONE;

        // Verify fiscal period
        FiscalPeriod period = fiscalPeriodRepository.findPeriodByDate(companyId, request.entryDate())
                .orElseThrow(() -> new BusinessException("NO_FISCAL_PERIOD", "No fiscal period found for entry date: " + request.entryDate()));

        JournalEntry entry = new JournalEntry();
        entry.setCompanyId(companyId);
        entry.setEntryDate(request.entryDate());
        entry.setFiscalPeriodId(period.getId());
        entry.setReference(request.reference());
        entry.setNarration(request.narration());
        entry.setSourceType(request.sourceType() != null ? request.sourceType() : JournalSourceType.MANUAL);
        entry.setSourceId(request.sourceId());
        entry.setCurrencyCode(currencyCode);
        entry.setExchangeRate(exchangeRate);

        // Build lines
        short lineNo = 1;
        BigDecimal sumDebit = BigDecimal.ZERO;
        BigDecimal sumCredit = BigDecimal.ZERO;

        for (JournalLineRequest lineReq : request.lines()) {
            BigDecimal debit = lineReq.debit() != null ? lineReq.debit().setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
            BigDecimal credit = lineReq.credit() != null ? lineReq.credit().setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO;

            JournalLine line = new JournalLine();
            line.setLineNo(lineNo++);
            line.setAccountId(lineReq.accountId());
            line.setCostCenterId(lineReq.costCenterId());
            line.setDescription(lineReq.description());
            line.setDebit(debit);
            line.setCredit(credit);
            line.setBaseDebit(debit.multiply(exchangeRate).setScale(2, RoundingMode.HALF_UP));
            line.setBaseCredit(credit.multiply(exchangeRate).setScale(2, RoundingMode.HALF_UP));
            line.setPartyType(lineReq.partyType());
            line.setPartyId(lineReq.partyId());

            entry.addLine(line);
            sumDebit = sumDebit.add(debit);
            sumCredit = sumCredit.add(credit);
        }

        entry.setTotalDebit(sumDebit);
        entry.setTotalCredit(sumCredit);

        boolean postImmediately = Boolean.TRUE.equals(request.postImmediately());
        if (postImmediately) {
            validatePostingRules(entry, period);
            entry.setStatus(JournalStatus.POSTED);
            entry.setPostedAt(LocalDateTime.now());
            entry.setPostedBy(CurrentUser.getUserId());
        } else {
            entry.setStatus(JournalStatus.DRAFT);
        }

        // Generate gap-free entry number
        String entryNo = numberSeriesService.generateNextNumber(companyId, DocType.JOURNAL);
        entry.setEntryNo(entryNo);

        JournalEntry saved = journalEntryRepository.save(entry);
        return ledgerMapper.toEntryDto(saved);
    }

    /**
     * Validates and posts an existing draft journal entry.
     */
    @Override
    @Transactional
    @PreAuthorize("hasAuthority('JOURNAL_POST')")
    public JournalEntryDto postJournalEntry(Long id) {
        Long companyId = CurrentUser.getCompanyId();

        JournalEntry entry = journalEntryRepository.findByCompanyIdAndId(companyId, id)
                .orElseThrow(() -> new BusinessException("JOURNAL_NOT_FOUND", "Journal entry not found with id: " + id));

        if (entry.getStatus() == JournalStatus.POSTED) {
            throw new BusinessException("ALREADY_POSTED", "Journal entry " + entry.getEntryNo() + " is already posted");
        }
        if (entry.getStatus() == JournalStatus.VOID) {
            throw new BusinessException("CANNOT_POST_VOID", "Cannot post a voided journal entry");
        }

        FiscalPeriod period = fiscalPeriodRepository.findById(entry.getFiscalPeriodId())
                .orElseThrow(() -> new BusinessException("NO_FISCAL_PERIOD", "Fiscal period not found"));

        validatePostingRules(entry, period);

        entry.setStatus(JournalStatus.POSTED);
        entry.setPostedAt(LocalDateTime.now());
        entry.setPostedBy(CurrentUser.getUserId());

        JournalEntry saved = journalEntryRepository.save(entry);
        return ledgerMapper.toEntryDto(saved);
    }

    /**
     * Core validation engine enforcing all double-entry and posting integrity rules.
     */
    @Override
    public void validatePostingRules(JournalEntry entry, FiscalPeriod period) {
        // 1. Minimum 2 lines
        if (entry.getLines() == null || entry.getLines().size() < 2) {
            throw new BusinessException("JOURNAL_MIN_LINES", "A journal entry must have at least two lines");
        }

        // 2. Fiscal period status check (block if CLOSED or LOCKED)
        if (period.getStatus() != FiscalPeriodStatus.OPEN) {
            throw new BusinessException("PERIOD_CLOSED", "Cannot post into a " + period.getStatus() + " fiscal period");
        }

        BigDecimal totalDebit = BigDecimal.ZERO;
        BigDecimal totalCredit = BigDecimal.ZERO;

        for (JournalLine line : entry.getLines()) {
            BigDecimal debit = line.getDebit() != null ? line.getDebit() : BigDecimal.ZERO;
            BigDecimal credit = line.getCredit() != null ? line.getCredit() : BigDecimal.ZERO;

            // 3. Line amounts must be non-negative
            if (debit.compareTo(BigDecimal.ZERO) < 0 || credit.compareTo(BigDecimal.ZERO) < 0) {
                throw new BusinessException("INVALID_LINE_AMOUNTS", "Line amounts cannot be negative");
            }

            // 4. Must have either debit or credit, but not both
            boolean hasDebit = debit.compareTo(BigDecimal.ZERO) > 0;
            boolean hasCredit = credit.compareTo(BigDecimal.ZERO) > 0;
            if ((hasDebit && hasCredit) || (!hasDebit && !hasCredit)) {
                throw new BusinessException("INVALID_LINE_AMOUNTS",
                        "Each line must have either a debit or a credit amount, but not both (Line " + line.getLineNo() + ")");
            }

            // 5. Account check: must exist, be active, and cannot be a group account
            Account account = accountRepository.findByCompanyIdAndId(entry.getCompanyId(), line.getAccountId())
                    .orElseThrow(() -> new BusinessException("ACCOUNT_NOT_FOUND", "Account not found for id: " + line.getAccountId()));

            if (Boolean.FALSE.equals(account.getIsActive())) {
                throw new BusinessException("ACCOUNT_INACTIVE", "Cannot post to inactive account: " + account.getCode());
            }

            if (Boolean.TRUE.equals(account.getIsGroup())) {
                throw new BusinessException("GROUP_ACCOUNT_CANNOT_POST",
                        "Cannot post to group account: " + account.getCode() + " (" + account.getName() + ") - only leaf accounts accept postings");
            }

            // 6. Cost center check: if provided, must exist, belong to company, and be active
            if (line.getCostCenterId() != null) {
                CostCenter cc = costCenterRepository.findByCompanyIdAndId(entry.getCompanyId(), line.getCostCenterId())
                        .orElseThrow(() -> new BusinessException("COST_CENTER_NOT_FOUND", "Cost center not found for id: " + line.getCostCenterId()));
                if (Boolean.FALSE.equals(cc.getIsActive())) {
                    throw new BusinessException("COST_CENTER_INACTIVE", "Cannot post to inactive cost center: " + cc.getCode());
                }
            }

            totalDebit = totalDebit.add(debit);
            totalCredit = totalCredit.add(credit);
        }

        // 7. Balanced double-entry check (total_debit == total_credit)
        if (totalDebit.compareTo(totalCredit) != 0) {
            throw new BusinessException("JOURNAL_UNBALANCED",
                    "Total debit (" + totalDebit + ") must strictly equal total credit (" + totalCredit + ")");
        }

        entry.setTotalDebit(totalDebit);
        entry.setTotalCredit(totalCredit);
    }

    /**
     * Voids a posted journal entry by generating a reversing entry linked via reversal_of_id.
     * The original entry is marked as VOID. Physical deletion is strictly prohibited.
     */
    @Override
    @Transactional
    @PreAuthorize("hasAuthority('JOURNAL_VOID')")
    public JournalEntryDto reverseJournalEntry(Long id, String reason) {
        Long companyId = CurrentUser.getCompanyId();

        JournalEntry original = journalEntryRepository.findByCompanyIdAndId(companyId, id)
                .orElseThrow(() -> new BusinessException("JOURNAL_NOT_FOUND", "Journal entry not found with id: " + id));

        if (original.getStatus() != JournalStatus.POSTED) {
            throw new BusinessException("CANNOT_REVERSE_UNPOSTED", "Only posted journal entries can be reversed. Current status: " + original.getStatus());
        }

        if (journalEntryRepository.existsByCompanyIdAndReversalOfId(companyId, id)) {
            throw new BusinessException("DUPLICATE_REVERSAL", "Journal entry " + original.getEntryNo() + " has already been reversed");
        }

        LocalDate reversalDate = LocalDate.now();
        FiscalPeriod period = fiscalPeriodRepository.findPeriodByDate(companyId, reversalDate)
                .orElseGet(() -> fiscalPeriodRepository.findById(original.getFiscalPeriodId())
                        .orElseThrow(() -> new BusinessException("NO_FISCAL_PERIOD", "No open fiscal period found for reversal")));

        if (period.getStatus() != FiscalPeriodStatus.OPEN) {
            throw new BusinessException("PERIOD_CLOSED", "Cannot reverse journal into a " + period.getStatus() + " fiscal period");
        }

        // Create reversing journal entry
        JournalEntry reversal = new JournalEntry();
        reversal.setCompanyId(companyId);
        reversal.setEntryDate(reversalDate);
        reversal.setFiscalPeriodId(period.getId());
        reversal.setReference(original.getEntryNo());
        reversal.setNarration("Reversal of " + original.getEntryNo() + (reason != null && !reason.isBlank() ? ": " + reason : ""));
        reversal.setSourceType(original.getSourceType());
        reversal.setSourceId(original.getSourceId());
        reversal.setCurrencyCode(original.getCurrencyCode());
        reversal.setExchangeRate(original.getExchangeRate());
        reversal.setTotalDebit(original.getTotalCredit()); // Swapped
        reversal.setTotalCredit(original.getTotalDebit()); // Swapped
        reversal.setStatus(JournalStatus.POSTED);
        reversal.setPostedAt(LocalDateTime.now());
        reversal.setPostedBy(CurrentUser.getUserId());
        reversal.setReversalOfId(original.getId());

        short lineNo = 1;
        for (JournalLine origLine : original.getLines()) {
            JournalLine revLine = new JournalLine();
            revLine.setLineNo(lineNo++);
            revLine.setAccountId(origLine.getAccountId());
            revLine.setCostCenterId(origLine.getCostCenterId());
            revLine.setDescription("Reversal: " + (origLine.getDescription() != null ? origLine.getDescription() : ""));
            // Swap debit and credit
            revLine.setDebit(origLine.getCredit());
            revLine.setCredit(origLine.getDebit());
            revLine.setBaseDebit(origLine.getBaseCredit());
            revLine.setBaseCredit(origLine.getBaseDebit());
            revLine.setPartyType(origLine.getPartyType());
            revLine.setPartyId(origLine.getPartyId());

            reversal.addLine(revLine);
        }

        String revEntryNo = numberSeriesService.generateNextNumber(companyId, DocType.JOURNAL);
        reversal.setEntryNo(revEntryNo);

        JournalEntry savedReversal = journalEntryRepository.save(reversal);

        // Mark original as VOID
        original.setStatus(JournalStatus.VOID);
        journalEntryRepository.save(original);

        return ledgerMapper.toEntryDto(savedReversal);
    }

    /**
     * Common reusable posting engine for all source documents (Invoices, Bills, Payments, Expenses, etc.).
     */
    @Override
    @Transactional
    public JournalEntryDto postDocument(SourceDocumentPostingRequest request) {
        Long companyId = CurrentUser.getCompanyId();

        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new BusinessException("COMPANY_NOT_FOUND", "Company not found for id: " + companyId));

        String currencyCode = request.currencyCode() != null ? request.currencyCode() : company.getBaseCurrency();
        BigDecimal exchangeRate = (request.exchangeRate() != null && request.exchangeRate().compareTo(BigDecimal.ZERO) > 0)
                ? request.exchangeRate() : BigDecimal.ONE;

        FiscalPeriod period = fiscalPeriodRepository.findPeriodByDate(companyId, request.entryDate())
                .orElseThrow(() -> new BusinessException("NO_FISCAL_PERIOD", "No fiscal period found for entry date: " + request.entryDate()));

        JournalEntry entry = new JournalEntry();
        entry.setCompanyId(companyId);
        entry.setEntryDate(request.entryDate());
        entry.setFiscalPeriodId(period.getId());
        entry.setReference(request.reference());
        entry.setNarration(request.narration());
        entry.setSourceType(request.sourceType());
        entry.setSourceId(request.sourceId());
        entry.setCurrencyCode(currencyCode);
        entry.setExchangeRate(exchangeRate);

        short lineNo = 1;
        BigDecimal sumDebit = BigDecimal.ZERO;
        BigDecimal sumCredit = BigDecimal.ZERO;

        for (JournalLineRequest lineReq : request.lines()) {
            BigDecimal debit = lineReq.debit() != null ? lineReq.debit().setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
            BigDecimal credit = lineReq.credit() != null ? lineReq.credit().setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO;

            JournalLine line = new JournalLine();
            line.setLineNo(lineNo++);
            line.setAccountId(lineReq.accountId());
            line.setCostCenterId(lineReq.costCenterId());
            line.setDescription(lineReq.description());
            line.setDebit(debit);
            line.setCredit(credit);
            line.setBaseDebit(debit.multiply(exchangeRate).setScale(2, RoundingMode.HALF_UP));
            line.setBaseCredit(credit.multiply(exchangeRate).setScale(2, RoundingMode.HALF_UP));
            line.setPartyType(lineReq.partyType());
            line.setPartyId(lineReq.partyId());

            entry.addLine(line);
            sumDebit = sumDebit.add(debit);
            sumCredit = sumCredit.add(credit);
        }

        entry.setTotalDebit(sumDebit);
        entry.setTotalCredit(sumCredit);

        validatePostingRules(entry, period);
        entry.setStatus(JournalStatus.POSTED);
        entry.setPostedAt(LocalDateTime.now());
        entry.setPostedBy(CurrentUser.getUserId());

        String entryNo = numberSeriesService.generateNextNumber(companyId, DocType.JOURNAL);
        entry.setEntryNo(entryNo);

        JournalEntry saved = journalEntryRepository.save(entry);
        return ledgerMapper.toEntryDto(saved);
    }
}

