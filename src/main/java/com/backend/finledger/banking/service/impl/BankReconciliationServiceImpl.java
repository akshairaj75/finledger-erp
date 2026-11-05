package com.backend.finledger.banking.service.impl;

import com.backend.finledger.banking.dto.*;
import com.backend.finledger.banking.entity.*;
import com.backend.finledger.banking.repository.BankAccountRepository;
import com.backend.finledger.banking.repository.BankReconciliationRepository;
import com.backend.finledger.banking.repository.BankStatementLineRepository;
import com.backend.finledger.banking.repository.BankStatementRepository;
import com.backend.finledger.banking.service.BankReconciliationService;
import com.backend.finledger.common.BusinessException;
import com.backend.finledger.ledger.dto.CreateJournalEntryRequest;
import com.backend.finledger.ledger.dto.JournalEntryDto;
import com.backend.finledger.ledger.dto.JournalLineDto;
import com.backend.finledger.ledger.dto.JournalLineRequest;
import com.backend.finledger.ledger.entity.Account;
import com.backend.finledger.ledger.entity.AccountType;
import com.backend.finledger.ledger.entity.JournalLine;
import com.backend.finledger.ledger.entity.JournalSourceType;
import com.backend.finledger.ledger.entity.NormalBalance;
import com.backend.finledger.ledger.repository.AccountRepository;
import com.backend.finledger.ledger.repository.JournalLineRepository;
import com.backend.finledger.ledger.service.PostingService;
import com.backend.finledger.security.CurrentUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class BankReconciliationServiceImpl implements BankReconciliationService {

    private final BankReconciliationRepository reconciliationRepository;
    private final BankAccountRepository bankAccountRepository;
    private final BankStatementRepository statementRepository;
    private final BankStatementLineRepository statementLineRepository;
    private final JournalLineRepository journalLineRepository;
    private final AccountRepository accountRepository;
    private final PostingService postingService;

    public BankReconciliationServiceImpl(
            BankReconciliationRepository reconciliationRepository,
            BankAccountRepository bankAccountRepository,
            BankStatementRepository statementRepository,
            BankStatementLineRepository statementLineRepository,
            JournalLineRepository journalLineRepository,
            AccountRepository accountRepository,
            PostingService postingService) {
        this.reconciliationRepository = reconciliationRepository;
        this.bankAccountRepository = bankAccountRepository;
        this.statementRepository = statementRepository;
        this.statementLineRepository = statementLineRepository;
        this.journalLineRepository = journalLineRepository;
        this.accountRepository = accountRepository;
        this.postingService = postingService;
    }

    @Override
    @Transactional
    public BankReconciliationDto startReconciliation(BankReconciliationRequest request) {
        Long companyId = CurrentUser.getCompanyId();
        BankAccount bankAccount = bankAccountRepository.findByCompanyIdAndId(companyId, request.bankAccountId())
                .orElseThrow(() -> new BusinessException("BANK_ACCOUNT_NOT_FOUND", "Bank account not found: " + request.bankAccountId()));

        Optional<BankReconciliation> existingOpt = reconciliationRepository
                .findByBankAccountIdAndStatementDate(bankAccount.getId(), request.statementDate());

        BankReconciliation recon;
        if (existingOpt.isPresent()) {
            recon = existingOpt.get();
            if (recon.getStatus() == ReconciliationStatus.COMPLETED) {
                return buildReconciliationDto(recon, bankAccount);
            }
            recon.setStatementBalance(request.statementBalance());
        } else {
            recon = new BankReconciliation();
            recon.setBankAccountId(bankAccount.getId());
            recon.setStatementDate(request.statementDate());
            recon.setStatementBalance(request.statementBalance());
            recon.setStatus(ReconciliationStatus.IN_PROGRESS);
        }

        recalculateBalances(recon, bankAccount);
        reconciliationRepository.save(recon);

        return buildReconciliationDto(recon, bankAccount);
    }

    @Override
    @Transactional(readOnly = true)
    public BankReconciliationDto getReconciliationById(Long id) {
        BankReconciliation recon = reconciliationRepository.findById(id)
                .orElseThrow(() -> new BusinessException("RECONCILIATION_NOT_FOUND", "Reconciliation not found: " + id));

        Long companyId = CurrentUser.getCompanyId();
        BankAccount bankAccount = bankAccountRepository.findByCompanyIdAndId(companyId, recon.getBankAccountId())
                .orElseThrow(() -> new BusinessException("BANK_ACCOUNT_NOT_FOUND", "Bank account not found: " + recon.getBankAccountId()));

        return buildReconciliationDto(recon, bankAccount);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BankReconciliationDto> getReconciliationsByBankAccountId(Long bankAccountId) {
        Long companyId = CurrentUser.getCompanyId();
        BankAccount bankAccount = bankAccountRepository.findByCompanyIdAndId(companyId, bankAccountId)
                .orElseThrow(() -> new BusinessException("BANK_ACCOUNT_NOT_FOUND", "Bank account not found: " + bankAccountId));

        return reconciliationRepository.findByBankAccountIdOrderByStatementDateDesc(bankAccountId).stream()
                .map(r -> buildReconciliationDto(r, bankAccount))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public BankReconciliationDto match(Long reconciliationId, ReconciliationMatchRequest request) {
        BankReconciliation recon = getReconciliationOrThrow(reconciliationId);
        assertInProgress(recon);

        BankAccount bankAccount = getBankAccountOrThrow(recon.getBankAccountId());

        BankStatementLine stmtLine = statementLineRepository.findById(request.statementLineId())
                .orElseThrow(() -> new BusinessException("STATEMENT_LINE_NOT_FOUND", "Statement line not found: " + request.statementLineId()));

        JournalLine journalLine = journalLineRepository.findById(request.journalLineId())
                .orElseThrow(() -> new BusinessException("JOURNAL_LINE_NOT_FOUND", "Journal line not found: " + request.journalLineId()));

        if (!journalLine.getAccountId().equals(bankAccount.getGlAccountId())) {
            throw new BusinessException("INVALID_GL_ACCOUNT", "Journal line does not belong to the bank GL account");
        }

        stmtLine.setMatchedJournalLineId(journalLine.getId());
        stmtLine.setMatchStatus(MatchStatus.MATCHED);
        statementLineRepository.save(stmtLine);

        journalLine.setIsReconciled(true);
        journalLineRepository.save(journalLine);

        recalculateBalances(recon, bankAccount);
        reconciliationRepository.save(recon);

        return buildReconciliationDto(recon, bankAccount);
    }

    @Override
    @Transactional
    public BankReconciliationDto unmatch(Long reconciliationId, Long statementLineId) {
        BankReconciliation recon = getReconciliationOrThrow(reconciliationId);
        assertInProgress(recon);

        BankAccount bankAccount = getBankAccountOrThrow(recon.getBankAccountId());

        BankStatementLine stmtLine = statementLineRepository.findById(statementLineId)
                .orElseThrow(() -> new BusinessException("STATEMENT_LINE_NOT_FOUND", "Statement line not found: " + statementLineId));

        if (stmtLine.getMatchedJournalLineId() != null) {
            journalLineRepository.findById(stmtLine.getMatchedJournalLineId()).ifPresent(jl -> {
                jl.setIsReconciled(false);
                journalLineRepository.save(jl);
            });
        }

        stmtLine.setMatchedJournalLineId(null);
        stmtLine.setMatchStatus(MatchStatus.UNMATCHED);
        statementLineRepository.save(stmtLine);

        recalculateBalances(recon, bankAccount);
        reconciliationRepository.save(recon);

        return buildReconciliationDto(recon, bankAccount);
    }

    @Override
    @Transactional
    public AutoMatchResultDto autoMatch(Long reconciliationId) {
        BankReconciliation recon = getReconciliationOrThrow(reconciliationId);
        assertInProgress(recon);

        BankAccount bankAccount = getBankAccountOrThrow(recon.getBankAccountId());
        Long companyId = CurrentUser.getCompanyId();

        List<BankStatementLine> stmtLines = getEligibleStatementLines(recon);
        List<BankStatementLine> unmatchedStmtLines = stmtLines.stream()
                .filter(l -> l.getMatchStatus() != MatchStatus.MATCHED)
                .collect(Collectors.toList());

        List<JournalLine> unreconciledBookLines = journalLineRepository
                .findUnreconciledLinesForAccount(companyId, bankAccount.getGlAccountId(), recon.getStatementDate());

        List<BankStatementLineDto> newlyMatched = new ArrayList<>();
        Set<Long> matchedJournalLineIds = new HashSet<>();

        for (BankStatementLine stmtLine : unmatchedStmtLines) {
            BigDecimal stmtAmount = stmtLine.getAmount();

            JournalLine candidate = null;
            for (JournalLine jl : unreconciledBookLines) {
                if (matchedJournalLineIds.contains(jl.getId())) {
                    continue;
                }

                // Check amount
                boolean amountMatches = false;
                if (stmtAmount.compareTo(BigDecimal.ZERO) > 0) {
                    // Deposit -> Book debit
                    amountMatches = stmtAmount.compareTo(jl.getBaseDebit()) == 0;
                } else if (stmtAmount.compareTo(BigDecimal.ZERO) < 0) {
                    // Withdrawal -> Book credit
                    amountMatches = stmtAmount.abs().compareTo(jl.getBaseCredit()) == 0;
                }

                if (amountMatches) {
                    LocalDate bookDate = jl.getJournalEntry().getEntryDate();
                    long daysDiff = Math.abs(ChronoUnit.DAYS.between(stmtLine.getTxnDate(), bookDate));
                    if (daysDiff <= 3) {
                        candidate = jl;
                        break;
                    }
                }
            }

            if (candidate != null) {
                stmtLine.setMatchedJournalLineId(candidate.getId());
                stmtLine.setMatchStatus(MatchStatus.MATCHED);
                statementLineRepository.save(stmtLine);

                candidate.setIsReconciled(true);
                journalLineRepository.save(candidate);

                matchedJournalLineIds.add(candidate.getId());
                newlyMatched.add(new BankStatementLineDto(
                        stmtLine.getId(),
                        stmtLine.getBankStatement().getId(),
                        stmtLine.getTxnDate(),
                        stmtLine.getDescription(),
                        stmtLine.getReference(),
                        stmtLine.getAmount(),
                        stmtLine.getMatchedJournalLineId(),
                        stmtLine.getMatchStatus()
                ));
            }
        }

        recalculateBalances(recon, bankAccount);
        reconciliationRepository.save(recon);

        BigDecimal reconciledTotal = newlyMatched.stream()
                .map(BankStatementLineDto::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new AutoMatchResultDto(
                newlyMatched.size(),
                reconciledTotal,
                recon.getDifference(),
                newlyMatched
        );
    }

    @Override
    @Transactional
    public BankReconciliationDto createBankCharge(Long reconciliationId, BankChargeRequest request) {
        BankReconciliation recon = getReconciliationOrThrow(reconciliationId);
        assertInProgress(recon);

        BankAccount bankAccount = getBankAccountOrThrow(recon.getBankAccountId());
        Long companyId = CurrentUser.getCompanyId();

        BankStatementLine stmtLine = statementLineRepository.findById(request.statementLineId())
                .orElseThrow(() -> new BusinessException("STATEMENT_LINE_NOT_FOUND", "Statement line not found: " + request.statementLineId()));

        BigDecimal chargeAmount = stmtLine.getAmount().abs();
        if (chargeAmount.compareTo(BigDecimal.ZERO) == 0) {
            throw new BusinessException("INVALID_CHARGE_AMOUNT", "Statement line amount cannot be zero");
        }

        Long expenseAccountId = request.expenseAccountId();
        if (expenseAccountId == null) {
            // Find default bank charges account
            expenseAccountId = accountRepository.findByCompanyIdOrderByCodeAsc(companyId).stream()
                    .filter(a -> a.getAccountType() == AccountType.EXPENSE)
                    .filter(a -> a.getName().toLowerCase().contains("bank") || a.getName().toLowerCase().contains("fee") || a.getCode().startsWith("63"))
                    .map(Account::getId)
                    .findFirst()
                    .orElseGet(() -> accountRepository.findByCompanyIdOrderByCodeAsc(companyId).stream()
                            .filter(a -> a.getAccountType() == AccountType.EXPENSE)
                            .map(Account::getId)
                            .findFirst()
                            .orElseThrow(() -> new BusinessException("EXPENSE_ACCOUNT_NOT_FOUND", "No suitable expense account found for bank charge")));
        }

        String narration = request.description() != null && !request.description().isBlank()
                ? request.description()
                : (stmtLine.getDescription() != null ? stmtLine.getDescription() : "Bank Fee / Charge");

        CreateJournalEntryRequest journalRequest = new CreateJournalEntryRequest(
                stmtLine.getTxnDate(),
                stmtLine.getReference() != null ? stmtLine.getReference() : "BANK-CHG-" + stmtLine.getId(),
                narration,
                JournalSourceType.MANUAL,
                stmtLine.getId(),
                bankAccount.getCurrencyCode(),
                BigDecimal.ONE,
                List.of(
                        new JournalLineRequest(
                                expenseAccountId,
                                null,
                                narration,
                                chargeAmount,
                                BigDecimal.ZERO,
                                null,
                                null
                        ),
                        new JournalLineRequest(
                                bankAccount.getGlAccountId(),
                                null,
                                narration,
                                BigDecimal.ZERO,
                                chargeAmount,
                                null,
                                null
                        )
                ),
                true // postImmediately
        );

        JournalEntryDto createdEntry = postingService.createJournalEntry(journalRequest);

        // Find the credit line on bank GL account
        JournalLineDto bankLine = createdEntry.lines().stream()
                .filter(l -> l.accountId().equals(bankAccount.getGlAccountId()))
                .findFirst()
                .orElseThrow(() -> new BusinessException("POSTING_ERROR", "Could not locate bank line in created journal entry"));

        stmtLine.setMatchedJournalLineId(bankLine.id());
        stmtLine.setMatchStatus(MatchStatus.MATCHED);
        statementLineRepository.save(stmtLine);

        journalLineRepository.findById(bankLine.id()).ifPresent(jl -> {
            jl.setIsReconciled(true);
            journalLineRepository.save(jl);
        });

        recalculateBalances(recon, bankAccount);
        reconciliationRepository.save(recon);

        return buildReconciliationDto(recon, bankAccount);
    }

    @Override
    @Transactional
    public BankReconciliationDto completeReconciliation(Long reconciliationId) {
        BankReconciliation recon = getReconciliationOrThrow(reconciliationId);
        assertInProgress(recon);

        BankAccount bankAccount = getBankAccountOrThrow(recon.getBankAccountId());
        recalculateBalances(recon, bankAccount);

        if (recon.getDifference().compareTo(BigDecimal.ZERO) != 0) {
            throw new BusinessException("RECONCILIATION_NOT_BALANCED",
                    "Reconciliation difference must be 0.00 to complete. Current difference: " + recon.getDifference());
        }

        recon.setStatus(ReconciliationStatus.COMPLETED);
        recon.setCompletedAt(Instant.now());
        reconciliationRepository.save(recon);

        return buildReconciliationDto(recon, bankAccount);
    }

    private void recalculateBalances(BankReconciliation recon, BankAccount bankAccount) {
        Long companyId = CurrentUser.getCompanyId();
        Account glAccount = accountRepository.findByCompanyIdAndId(companyId, bankAccount.getGlAccountId())
                .orElseThrow(() -> new BusinessException("GL_ACCOUNT_NOT_FOUND", "GL account not found: " + bankAccount.getGlAccountId()));

        BigDecimal initialOpening = glAccount.getOpeningBalance() != null ? glAccount.getOpeningBalance() : BigDecimal.ZERO;

        List<JournalLine> allPostedLines = journalLineRepository
                .findAllPostedLinesForAccount(companyId, bankAccount.getGlAccountId(), recon.getStatementDate());

        BigDecimal reconciledDebits = allPostedLines.stream()
                .filter(jl -> Boolean.TRUE.equals(jl.getIsReconciled()))
                .map(JournalLine::getBaseDebit)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal reconciledCredits = allPostedLines.stream()
                .filter(jl -> Boolean.TRUE.equals(jl.getIsReconciled()))
                .map(JournalLine::getBaseCredit)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal clearedBookBalance;
        if (glAccount.getNormalBalance() == NormalBalance.DEBIT) {
            clearedBookBalance = initialOpening.add(reconciledDebits).subtract(reconciledCredits);
        } else {
            clearedBookBalance = initialOpening.add(reconciledCredits).subtract(reconciledDebits);
        }

        recon.setBookBalance(clearedBookBalance);
        BigDecimal difference = recon.getStatementBalance().subtract(clearedBookBalance);
        recon.setDifference(difference);
    }

    private BankReconciliationDto buildReconciliationDto(BankReconciliation recon, BankAccount bankAccount) {
        Long companyId = CurrentUser.getCompanyId();

        List<BankStatementLine> stmtLines = getEligibleStatementLines(recon);
        List<BankStatementLineDto> statementLineDtos = stmtLines.stream()
                .map(l -> new BankStatementLineDto(
                        l.getId(),
                        l.getBankStatement().getId(),
                        l.getTxnDate(),
                        l.getDescription(),
                        l.getReference(),
                        l.getAmount(),
                        l.getMatchedJournalLineId(),
                        l.getMatchStatus()
                ))
                .collect(Collectors.toList());

        List<JournalLine> unreconciledLines = journalLineRepository
                .findUnreconciledLinesForAccount(companyId, bankAccount.getGlAccountId(), recon.getStatementDate());

        // Also fetch matched journal lines for this reconciliation so they appear in list
        Set<Long> matchedJlIds = stmtLines.stream()
                .map(BankStatementLine::getMatchedJournalLineId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        List<JournalLine> matchedLines = matchedJlIds.isEmpty() ? List.of()
                : journalLineRepository.findAllById(matchedJlIds);

        Map<Long, JournalLine> combinedMap = new LinkedHashMap<>();
        for (JournalLine jl : unreconciledLines) {
            combinedMap.put(jl.getId(), jl);
        }
        for (JournalLine jl : matchedLines) {
            combinedMap.put(jl.getId(), jl);
        }

        List<BookTransactionDto> bookTransactionDtos = combinedMap.values().stream()
                .sorted(Comparator.comparing((JournalLine jl) -> jl.getJournalEntry().getEntryDate()).thenComparing(JournalLine::getId))
                .map(jl -> new BookTransactionDto(
                        jl.getId(),
                        jl.getJournalEntry().getId(),
                        jl.getJournalEntry().getEntryNo(),
                        jl.getJournalEntry().getEntryDate(),
                        jl.getJournalEntry().getReference(),
                        jl.getDescription() != null ? jl.getDescription() : jl.getJournalEntry().getNarration(),
                        jl.getBaseDebit(),
                        jl.getBaseCredit(),
                        jl.getBaseDebit().subtract(jl.getBaseCredit()),
                        jl.getIsReconciled()
                ))
                .collect(Collectors.toList());

        int matchedCount = (int) stmtLines.stream().filter(l -> l.getMatchStatus() == MatchStatus.MATCHED).count();
        int unmatchedStmt = (int) stmtLines.stream().filter(l -> l.getMatchStatus() != MatchStatus.MATCHED).count();
        int unmatchedBook = (int) bookTransactionDtos.stream().filter(b -> !Boolean.TRUE.equals(b.isReconciled())).count();

        return new BankReconciliationDto(
                recon.getId(),
                recon.getBankAccountId(),
                bankAccount.getName(),
                bankAccount.getGlAccountId(),
                recon.getStatementDate(),
                recon.getStatementBalance(),
                recon.getBookBalance(),
                recon.getDifference(),
                recon.getStatus(),
                recon.getCompletedAt(),
                statementLineDtos,
                bookTransactionDtos,
                matchedCount,
                unmatchedStmt,
                unmatchedBook
        );
    }

    private List<BankStatementLine> getEligibleStatementLines(BankReconciliation recon) {
        List<BankStatement> statements = statementRepository
                .findByBankAccountIdOrderByStatementDateDesc(recon.getBankAccountId());

        List<BankStatementLine> allLines = new ArrayList<>();
        for (BankStatement stmt : statements) {
            for (BankStatementLine line : stmt.getLines()) {
                if (line.getTxnDate().isBefore(recon.getStatementDate()) || line.getTxnDate().isEqual(recon.getStatementDate())) {
                    allLines.add(line);
                }
            }
        }
        allLines.sort(Comparator.comparing(BankStatementLine::getTxnDate).thenComparing(BankStatementLine::getId));
        return allLines;
    }

    private BankReconciliation getReconciliationOrThrow(Long id) {
        return reconciliationRepository.findById(id)
                .orElseThrow(() -> new BusinessException("RECONCILIATION_NOT_FOUND", "Reconciliation not found: " + id));
    }

    private BankAccount getBankAccountOrThrow(Long bankAccountId) {
        Long companyId = CurrentUser.getCompanyId();
        return bankAccountRepository.findByCompanyIdAndId(companyId, bankAccountId)
                .orElseThrow(() -> new BusinessException("BANK_ACCOUNT_NOT_FOUND", "Bank account not found: " + bankAccountId));
    }

    private void assertInProgress(BankReconciliation recon) {
        if (recon.getStatus() == ReconciliationStatus.COMPLETED) {
            throw new BusinessException("RECONCILIATION_ALREADY_COMPLETED", "Cannot modify a completed reconciliation");
        }
    }
}
