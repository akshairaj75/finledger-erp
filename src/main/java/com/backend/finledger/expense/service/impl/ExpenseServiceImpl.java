package com.backend.finledger.expense.service.impl;

import com.backend.finledger.attachment.dto.AttachmentDto;
import com.backend.finledger.attachment.repository.AttachmentRepository;
import com.backend.finledger.banking.entity.BankAccount;
import com.backend.finledger.banking.repository.BankAccountRepository;
import com.backend.finledger.common.BusinessException;
import com.backend.finledger.common.DocType;
import com.backend.finledger.common.NumberSeriesService;
import com.backend.finledger.common.PageResponse;
import com.backend.finledger.company.entity.Company;
import com.backend.finledger.company.repository.CompanyRepository;
import com.backend.finledger.expense.dto.*;
import com.backend.finledger.expense.entity.*;
import com.backend.finledger.expense.repository.ExpenseApprovalRepository;
import com.backend.finledger.expense.repository.ExpenseCategoryRepository;
import com.backend.finledger.expense.repository.ExpenseLineRepository;
import com.backend.finledger.expense.repository.ExpenseRepository;
import com.backend.finledger.expense.service.ApprovalPolicy;
import com.backend.finledger.expense.service.ExpenseService;
import com.backend.finledger.ledger.dto.JournalEntryDto;
import com.backend.finledger.ledger.dto.JournalLineRequest;
import com.backend.finledger.ledger.dto.SourceDocumentPostingRequest;
import com.backend.finledger.ledger.entity.*;
import com.backend.finledger.ledger.repository.AccountRepository;
import com.backend.finledger.ledger.repository.JournalEntryRepository;
import com.backend.finledger.ledger.service.PostingService;
import com.backend.finledger.purchases.entity.Vendor;
import com.backend.finledger.purchases.repository.VendorRepository;
import com.backend.finledger.security.CurrentUser;
import com.backend.finledger.tax.entity.TaxRate;
import com.backend.finledger.tax.repository.TaxRateRepository;
import com.backend.finledger.user.entity.User;
import com.backend.finledger.user.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

@Service
public class ExpenseServiceImpl implements ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final ExpenseLineRepository expenseLineRepository;
    private final ExpenseApprovalRepository expenseApprovalRepository;
    private final ExpenseCategoryRepository categoryRepository;
    private final AccountRepository accountRepository;
    private final TaxRateRepository taxRateRepository;
    private final BankAccountRepository bankAccountRepository;
    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;
    private final VendorRepository vendorRepository;
    private final JournalEntryRepository journalEntryRepository;
    private final PostingService postingService;
    private final NumberSeriesService numberSeriesService;
    private final ApprovalPolicy approvalPolicy;
    private final AttachmentRepository attachmentRepository;

    public ExpenseServiceImpl(
            ExpenseRepository expenseRepository,
            ExpenseLineRepository expenseLineRepository,
            ExpenseApprovalRepository expenseApprovalRepository,
            ExpenseCategoryRepository categoryRepository,
            AccountRepository accountRepository,
            TaxRateRepository taxRateRepository,
            BankAccountRepository bankAccountRepository,
            CompanyRepository companyRepository,
            UserRepository userRepository,
            VendorRepository vendorRepository,
            JournalEntryRepository journalEntryRepository,
            PostingService postingService,
            NumberSeriesService numberSeriesService,
            ApprovalPolicy approvalPolicy,
            AttachmentRepository attachmentRepository) {
        this.expenseRepository = expenseRepository;
        this.expenseLineRepository = expenseLineRepository;
        this.expenseApprovalRepository = expenseApprovalRepository;
        this.categoryRepository = categoryRepository;
        this.accountRepository = accountRepository;
        this.taxRateRepository = taxRateRepository;
        this.bankAccountRepository = bankAccountRepository;
        this.companyRepository = companyRepository;
        this.userRepository = userRepository;
        this.vendorRepository = vendorRepository;
        this.journalEntryRepository = journalEntryRepository;
        this.postingService = postingService;
        this.numberSeriesService = numberSeriesService;
        this.approvalPolicy = approvalPolicy;
        this.attachmentRepository = attachmentRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ExpenseDto> getExpenses(
            String tab,
            ExpenseStatus status,
            LocalDate from,
            LocalDate to,
            Pageable pageable) {
        Long companyId = CurrentUser.getCompanyId();
        Long currentUserId = CurrentUser.getUserId();

        PaidBy filterPaidBy = null;
        Long filterEmployeeId = null;
        ExpenseStatus filterStatus = status;

        if (tab != null) {
            switch (tab.trim().toLowerCase(Locale.ROOT)) {
                case "mine" -> filterEmployeeId = currentUserId;
                case "approvals" -> filterStatus = ExpenseStatus.SUBMITTED;
                case "reimburse" -> {
                    filterPaidBy = PaidBy.EMPLOYEE;
                    filterStatus = ExpenseStatus.POSTED;
                }
                case "all" -> {}
            }
        }

        Page<Expense> page = expenseRepository.searchExpenses(
                companyId, filterStatus, filterPaidBy, filterEmployeeId, from, to, pageable);

        List<ExpenseDto> dtos = page.getContent().stream().map(this::toDto).toList();
        return new PageResponse<>(dtos, page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }

    @Override
    @Transactional(readOnly = true)
    public ExpenseDto getExpenseById(Long id) {
        Long companyId = CurrentUser.getCompanyId();
        Expense expense = expenseRepository.findByCompanyIdAndId(companyId, id)
                .orElseThrow(() -> new BusinessException("EXPENSE_NOT_FOUND", "Expense not found with id: " + id));
        return toDto(expense);
    }

    @Override
    @Transactional
    public ExpenseDto createExpense(CreateExpenseRequest request) {
        Long companyId = CurrentUser.getCompanyId();
        Long currentUserId = CurrentUser.getUserId();

        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new BusinessException("COMPANY_NOT_FOUND", "Company not found with id: " + companyId));

        String currencyCode = request.currencyCode() != null && !request.currencyCode().isBlank()
                ? request.currencyCode().toUpperCase(Locale.ROOT)
                : company.getBaseCurrency();

        BigDecimal exchangeRate = request.exchangeRate() != null && request.exchangeRate().compareTo(BigDecimal.ZERO) > 0
                ? request.exchangeRate()
                : BigDecimal.ONE;

        Expense expense = new Expense();
        expense.setCompanyId(companyId);
        expense.setExpenseDate(request.expenseDate());
        expense.setPaidBy(request.paidBy());
        expense.setCurrencyCode(currencyCode);
        expense.setExchangeRate(exchangeRate);
        expense.setVendorId(request.vendorId());
        expense.setPayeeName(request.payeeName());

        if (request.paidBy() == PaidBy.EMPLOYEE) {
            Long empId = request.employeeUserId() != null ? request.employeeUserId() : currentUserId;
            expense.setEmployeeUserId(empId);
            expense.setStatus(ExpenseStatus.DRAFT);
        } else {
            // COMPANY paid: bank/card/cash account is required
            if (request.bankAccountId() == null) {
                throw new BusinessException("BANK_ACCOUNT_REQUIRED", "Bank/Cash/Card account is required for company-paid expense.");
            }
            BankAccount bankAccount = bankAccountRepository.findByCompanyIdAndId(companyId, request.bankAccountId())
                    .orElseThrow(() -> new BusinessException("BANK_ACCOUNT_NOT_FOUND", "Bank account not found with id: " + request.bankAccountId()));
            expense.setBankAccountId(bankAccount.getId());
        }

        String expenseNo = numberSeriesService.generateNextNumber(companyId, DocType.EXPENSE);
        expense.setExpenseNo(expenseNo);

        populateLinesAndTotals(expense, request.lines(), companyId);

        Expense saved = expenseRepository.save(expense);

        // Posting rule: Company-paid expenses post immediately upon save
        if (saved.getPaidBy() == PaidBy.COMPANY) {
            postCompanyExpense(saved);
        }

        return toDto(saved);
    }

    @Override
    @Transactional
    public ExpenseDto updateExpense(Long id, UpdateExpenseRequest request) {
        Long companyId = CurrentUser.getCompanyId();

        Expense expense = expenseRepository.findByCompanyIdAndId(companyId, id)
                .orElseThrow(() -> new BusinessException("EXPENSE_NOT_FOUND", "Expense not found with id: " + id));

        if (expense.getStatus() != ExpenseStatus.DRAFT && expense.getStatus() != ExpenseStatus.REJECTED) {
            throw new BusinessException("EXPENSE_LOCKED", "Only DRAFT or REJECTED expenses can be edited. Current status: " + expense.getStatus());
        }

        if (request.expenseDate() != null) expense.setExpenseDate(request.expenseDate());
        if (request.paidBy() != null) expense.setPaidBy(request.paidBy());
        if (request.employeeUserId() != null) expense.setEmployeeUserId(request.employeeUserId());
        if (request.vendorId() != null) expense.setVendorId(request.vendorId());
        if (request.payeeName() != null) expense.setPayeeName(request.payeeName());
        if (request.bankAccountId() != null) expense.setBankAccountId(request.bankAccountId());
        if (request.currencyCode() != null && !request.currencyCode().isBlank()) expense.setCurrencyCode(request.currencyCode().toUpperCase(Locale.ROOT));
        if (request.exchangeRate() != null && request.exchangeRate().compareTo(BigDecimal.ZERO) > 0) expense.setExchangeRate(request.exchangeRate());

        if (request.lines() != null && !request.lines().isEmpty()) {
            expense.getLines().clear();
            populateLinesAndTotals(expense, request.lines(), companyId);
        }

        Expense saved = expenseRepository.save(expense);
        return toDto(saved);
    }

    @Override
    @Transactional
    public ExpenseDto submitExpense(Long id) {
        Long companyId = CurrentUser.getCompanyId();

        Expense expense = expenseRepository.findByCompanyIdAndId(companyId, id)
                .orElseThrow(() -> new BusinessException("EXPENSE_NOT_FOUND", "Expense not found with id: " + id));

        if (expense.getStatus() != ExpenseStatus.DRAFT && expense.getStatus() != ExpenseStatus.REJECTED) {
            throw new BusinessException("INVALID_STATUS", "Only DRAFT or REJECTED claims can be submitted. Current status: " + expense.getStatus());
        }

        // Policy checks upon submission (receipt required, per-claim limit)
        approvalPolicy.validateSubmission(expense);

        expense.setStatus(ExpenseStatus.SUBMITTED);
        expense.setSubmittedAt(Instant.now());
        expense.setRejectionReason(null);

        Expense saved = expenseRepository.save(expense);
        return toDto(saved);
    }

    @Override
    @Transactional
    public ExpenseDto approveExpense(Long id, String comment) {
        Long companyId = CurrentUser.getCompanyId();
        Long currentUserId = CurrentUser.getUserId();

        Expense expense = expenseRepository.findByCompanyIdAndId(companyId, id)
                .orElseThrow(() -> new BusinessException("EXPENSE_NOT_FOUND", "Expense not found with id: " + id));

        if (expense.getStatus() != ExpenseStatus.SUBMITTED) {
            throw new BusinessException("INVALID_STATUS", "Only SUBMITTED expenses can be approved. Current status: " + expense.getStatus());
        }

        // Self-approval enforcement: HTTP 403 SELF_APPROVAL_NOT_ALLOWED
        approvalPolicy.validateApproval(expense, currentUserId);

        User approverUser = userRepository.findById(currentUserId).orElse(null);
        String approverName = approverUser != null ? approverUser.getFullName() : "Approver #" + currentUserId;

        List<ExpenseApproval> existingApprovals = expenseApprovalRepository.findByExpenseIdOrderByStepAsc(expense.getId());
        int nextStep = existingApprovals.size() + 1;
        int requiredSteps = approvalPolicy.getRequiredApprovalSteps(expense);

        ExpenseApproval approval = new ExpenseApproval(
                expense.getId(),
                nextStep,
                currentUserId,
                approverName,
                ApprovalDecision.APPROVED,
                comment
        );
        expenseApprovalRepository.save(approval);

        if (nextStep >= requiredSteps) {
            // Final approval reached -> Post to general ledger
            expense.setStatus(ExpenseStatus.POSTED);
            expense.setApprovedBy(currentUserId);
            expense.setApprovedAt(Instant.now());

            if (expense.getPaidBy() == PaidBy.EMPLOYEE) {
                postEmployeeClaim(expense);
            } else if (expense.getJournalEntryId() == null) {
                postCompanyExpense(expense);
            }
        } else {
            // Additional review step required (e.g. Finance review)
            expense.setStatus(ExpenseStatus.SUBMITTED);
        }

        Expense saved = expenseRepository.save(expense);
        return toDto(saved);
    }

    @Override
    @Transactional
    public ExpenseDto rejectExpense(Long id, String reason) {
        Long companyId = CurrentUser.getCompanyId();
        Long currentUserId = CurrentUser.getUserId();

        Expense expense = expenseRepository.findByCompanyIdAndId(companyId, id)
                .orElseThrow(() -> new BusinessException("EXPENSE_NOT_FOUND", "Expense not found with id: " + id));

        if (expense.getStatus() != ExpenseStatus.SUBMITTED) {
            throw new BusinessException("INVALID_STATUS", "Only SUBMITTED expenses can be rejected. Current status: " + expense.getStatus());
        }

        approvalPolicy.validateApproval(expense, currentUserId);

        User approverUser = userRepository.findById(currentUserId).orElse(null);
        String approverName = approverUser != null ? approverUser.getFullName() : "Approver #" + currentUserId;

        List<ExpenseApproval> existingApprovals = expenseApprovalRepository.findByExpenseIdOrderByStepAsc(expense.getId());
        int nextStep = existingApprovals.size() + 1;

        ExpenseApproval approval = new ExpenseApproval(
                expense.getId(),
                nextStep,
                currentUserId,
                approverName,
                ApprovalDecision.REJECTED,
                reason
        );
        expenseApprovalRepository.save(approval);

        expense.setStatus(ExpenseStatus.REJECTED);
        expense.setRejectionReason(reason);

        Expense saved = expenseRepository.save(expense);
        return toDto(saved);
    }

    private void populateLinesAndTotals(Expense expense, List<ExpenseLineRequest> lineRequests, Long companyId) {
        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal taxTotal = BigDecimal.ZERO;
        short lineCounter = 1;

        for (ExpenseLineRequest req : lineRequests) {
            ExpenseCategory cat = categoryRepository.findByCompanyIdAndId(companyId, req.categoryId())
                    .orElseThrow(() -> new BusinessException("CATEGORY_NOT_FOUND", "Category not found with id: " + req.categoryId()));

            Long accountId = req.accountId() != null ? req.accountId() : cat.getAccountId();
            Account account = accountRepository.findByCompanyIdAndId(companyId, accountId)
                    .orElseThrow(() -> new BusinessException("ACCOUNT_NOT_FOUND", "Account not found with id: " + accountId));

            if (Boolean.TRUE.equals(account.getIsGroup())) {
                throw new BusinessException("INVALID_ACCOUNT", "Cannot post to group account: " + account.getCode());
            }

            BigDecimal amount = req.amount().setScale(2, RoundingMode.HALF_UP);
            BigDecimal taxAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

            Long taxRateId = req.taxRateId() != null ? req.taxRateId() : cat.getDefaultTaxRateId();
            if (taxRateId != null) {
                TaxRate taxRate = taxRateRepository.findByCompanyIdAndId(companyId, taxRateId)
                        .orElseThrow(() -> new BusinessException("TAX_RATE_NOT_FOUND", "Tax rate not found with id: " + taxRateId));
                taxAmount = amount.multiply(taxRate.getRate())
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            }

            ExpenseLine line = new ExpenseLine();
            line.setLineNo(req.lineNo() != null ? req.lineNo() : lineCounter);
            line.setCategoryId(cat.getId());
            line.setAccountId(accountId);
            line.setDescription(req.description().trim());
            line.setAmount(amount);
            line.setTaxRateId(taxRateId);
            line.setTaxAmount(taxAmount);
            line.setCostCenterId(req.costCenterId());
            line.setCustomerId(req.customerId());
            line.setIsBillable(Boolean.TRUE.equals(req.isBillable()));

            expense.addLine(line);

            subtotal = subtotal.add(amount);
            taxTotal = taxTotal.add(taxAmount);
            lineCounter++;
        }

        expense.setSubtotal(subtotal);
        expense.setTaxTotal(taxTotal);
        expense.setTotal(subtotal.add(taxTotal));
    }

    private void postCompanyExpense(Expense expense) {
        Long companyId = expense.getCompanyId();
        BankAccount bankAccount = bankAccountRepository.findByCompanyIdAndId(companyId, expense.getBankAccountId())
                .orElseThrow(() -> new BusinessException("BANK_ACCOUNT_NOT_FOUND", "Bank account not found: " + expense.getBankAccountId()));

        List<JournalLineRequest> journalLines = new ArrayList<>();

        // Dr Expense per line
        for (ExpenseLine line : expense.getLines()) {
            journalLines.add(new JournalLineRequest(
                    line.getAccountId(),
                    line.getCostCenterId(),
                    "Expense: " + line.getDescription(),
                    line.getAmount(),
                    BigDecimal.ZERO,
                    null,
                    null
            ));

            // Dr Input VAT if tax applies
            if (line.getTaxAmount() != null && line.getTaxAmount().compareTo(BigDecimal.ZERO) > 0) {
                Long vatAccountId = resolvePurchaseVatAccount(companyId, line.getTaxRateId());
                journalLines.add(new JournalLineRequest(
                        vatAccountId,
                        line.getCostCenterId(),
                        "Input VAT - " + line.getDescription(),
                        line.getTaxAmount(),
                        BigDecimal.ZERO,
                        null,
                        null
                ));
            }
        }

        // Cr Bank / Cash / Card account
        journalLines.add(new JournalLineRequest(
                bankAccount.getGlAccountId(),
                null,
                "Payment - " + expense.getExpenseNo() + (expense.getPayeeName() != null ? " (" + expense.getPayeeName() + ")" : ""),
                BigDecimal.ZERO,
                expense.getTotal(),
                null,
                null
        ));

        SourceDocumentPostingRequest postingRequest = new SourceDocumentPostingRequest(
                JournalSourceType.EXPENSE,
                expense.getId(),
                expense.getExpenseDate(),
                expense.getExpenseNo(),
                "Company Expense #" + expense.getExpenseNo() + (expense.getPayeeName() != null ? " - " + expense.getPayeeName() : ""),
                expense.getCurrencyCode(),
                expense.getExchangeRate(),
                journalLines
        );

        JournalEntryDto postedEntry = postingService.postDocument(postingRequest);
        expense.setJournalEntryId(postedEntry.id());
        expense.setStatus(ExpenseStatus.POSTED);
    }

    private void postEmployeeClaim(Expense expense) {
        Long companyId = expense.getCompanyId();
        Long reimbursementsPayableAccountId = getOrCreateEmployeeReimbursementsAccount(companyId);

        List<JournalLineRequest> journalLines = new ArrayList<>();

        // Dr Expense per line
        for (ExpenseLine line : expense.getLines()) {
            journalLines.add(new JournalLineRequest(
                    line.getAccountId(),
                    line.getCostCenterId(),
                    "Claim: " + line.getDescription(),
                    line.getAmount(),
                    BigDecimal.ZERO,
                    null,
                    null
            ));

            // Dr Input VAT if tax applies
            if (line.getTaxAmount() != null && line.getTaxAmount().compareTo(BigDecimal.ZERO) > 0) {
                Long vatAccountId = resolvePurchaseVatAccount(companyId, line.getTaxRateId());
                journalLines.add(new JournalLineRequest(
                        vatAccountId,
                        line.getCostCenterId(),
                        "Input VAT - " + line.getDescription(),
                        line.getTaxAmount(),
                        BigDecimal.ZERO,
                        null,
                        null
                ));
            }
        }

        // Cr Employee Reimbursements Payable
        journalLines.add(new JournalLineRequest(
                reimbursementsPayableAccountId,
                null,
                "Employee Reimbursements Payable - " + expense.getExpenseNo(),
                BigDecimal.ZERO,
                expense.getTotal(),
                null,
                null
        ));

        SourceDocumentPostingRequest postingRequest = new SourceDocumentPostingRequest(
                JournalSourceType.EXPENSE,
                expense.getId(),
                expense.getExpenseDate(),
                expense.getExpenseNo(),
                "Employee Claim #" + expense.getExpenseNo(),
                expense.getCurrencyCode(),
                expense.getExchangeRate(),
                journalLines
        );

        JournalEntryDto postedEntry = postingService.postDocument(postingRequest);
        expense.setJournalEntryId(postedEntry.id());
    }

    private Long getOrCreateEmployeeReimbursementsAccount(Long companyId) {
        Optional<Account> byCode = accountRepository.findByCompanyIdAndCode(companyId, "2150");
        if (byCode.isPresent()) {
            return byCode.get().getId();
        }

        List<Account> accounts = accountRepository.findByCompanyIdOrderByCodeAsc(companyId);
        for (Account a : accounts) {
            if (a.getName() != null && a.getName().toLowerCase(Locale.ROOT).contains("reimbursement")
                    && Boolean.TRUE.equals(a.getIsActive()) && !Boolean.TRUE.equals(a.getIsGroup())) {
                return a.getId();
            }
        }

        Account liabilities = accountRepository.findByCompanyIdAndCode(companyId, "2000").orElse(null);
        Account newAcc = new Account(
                companyId,
                "2150",
                "Employee Reimbursements Payable",
                AccountType.LIABILITY,
                "PAYABLE",
                liabilities != null ? liabilities.getId() : null,
                false,
                NormalBalance.CREDIT
        );
        newAcc.setIsSystem(true);
        newAcc.setIsActive(true);
        return accountRepository.save(newAcc).getId();
    }

    private Long resolvePurchaseVatAccount(Long companyId, Long taxRateId) {
        if (taxRateId != null) {
            TaxRate taxRate = taxRateRepository.findByCompanyIdAndId(companyId, taxRateId).orElse(null);
            if (taxRate != null && taxRate.getPurchaseAccountId() != null) {
                return taxRate.getPurchaseAccountId();
            }
        }

        List<Account> accounts = accountRepository.findByCompanyIdOrderByCodeAsc(companyId);
        for (Account a : accounts) {
            if (a.getName() != null && (a.getName().toLowerCase(Locale.ROOT).contains("input vat")
                    || a.getName().toLowerCase(Locale.ROOT).contains("vat receivable"))
                    && Boolean.TRUE.equals(a.getIsActive()) && !Boolean.TRUE.equals(a.getIsGroup())) {
                return a.getId();
            }
        }

        Optional<Account> acc1400 = accountRepository.findByCompanyIdAndCode(companyId, "1400");
        if (acc1400.isPresent()) return acc1400.get().getId();
        Optional<Account> acc2200 = accountRepository.findByCompanyIdAndCode(companyId, "2200");
        if (acc2200.isPresent()) return acc2200.get().getId();

        Account assets = accountRepository.findByCompanyIdAndCode(companyId, "1000").orElse(null);
        Account newVat = new Account(
                companyId,
                "1400",
                "Input VAT (Recoverable)",
                AccountType.ASSET,
                "TAX",
                assets != null ? assets.getId() : null,
                false,
                NormalBalance.DEBIT
        );
        newVat.setIsSystem(true);
        newVat.setIsActive(true);
        return accountRepository.save(newVat).getId();
    }

    private ExpenseDto toDto(Expense exp) {
        String employeeName = null;
        if (exp.getEmployeeUserId() != null) {
            employeeName = userRepository.findById(exp.getEmployeeUserId())
                    .map(User::getFullName)
                    .orElse(null);
        }

        String approverName = null;
        if (exp.getApprovedBy() != null) {
            approverName = userRepository.findById(exp.getApprovedBy())
                    .map(User::getFullName)
                    .orElse(null);
        }

        String vendorName = null;
        if (exp.getVendorId() != null) {
            vendorName = vendorRepository.findByCompanyIdAndId(exp.getCompanyId(), exp.getVendorId())
                    .map(Vendor::getName)
                    .orElse(null);
        }

        String bankAccountName = null;
        if (exp.getBankAccountId() != null) {
            bankAccountName = bankAccountRepository.findByCompanyIdAndId(exp.getCompanyId(), exp.getBankAccountId())
                    .map(BankAccount::getName)
                    .orElse(null);
        }

        String journalEntryNo = null;
        if (exp.getJournalEntryId() != null) {
            journalEntryNo = journalEntryRepository.findById(exp.getJournalEntryId())
                    .map(JournalEntry::getEntryNo)
                    .orElse(null);
        }

        String reimbursementEntryNo = null;
        if (exp.getReimbursementEntryId() != null) {
            reimbursementEntryNo = journalEntryRepository.findById(exp.getReimbursementEntryId())
                    .map(JournalEntry::getEntryNo)
                    .orElse(null);
        }

        List<ExpenseLineDto> lineDtos = exp.getLines().stream().map(line -> {
            String catName = categoryRepository.findByCompanyIdAndId(exp.getCompanyId(), line.getCategoryId())
                    .map(ExpenseCategory::getName).orElse(null);
            Account acc = accountRepository.findByCompanyIdAndId(exp.getCompanyId(), line.getAccountId()).orElse(null);
            String taxRateCode = null;
            if (line.getTaxRateId() != null) {
                taxRateCode = taxRateRepository.findByCompanyIdAndId(exp.getCompanyId(), line.getTaxRateId())
                        .map(TaxRate::getCode).orElse(null);
            }

            return new ExpenseLineDto(
                    line.getId(),
                    line.getLineNo(),
                    line.getCategoryId(),
                    catName,
                    line.getAccountId(),
                    acc != null ? acc.getCode() : null,
                    acc != null ? acc.getName() : null,
                    line.getDescription(),
                    line.getAmount(),
                    line.getTaxRateId(),
                    taxRateCode,
                    line.getTaxAmount(),
                    line.getCostCenterId(),
                    line.getCustomerId(),
                    line.getIsBillable()
            );
        }).toList();

        List<AttachmentDto> attachmentDtos = attachmentRepository.findByCompanyIdAndEntityTypeAndEntityId(
                exp.getCompanyId(), "EXPENSE", exp.getId()).stream().map(a -> new AttachmentDto(
                a.getId(),
                a.getCompanyId(),
                a.getEntityType(),
                a.getEntityId(),
                a.getFileName(),
                a.getContentType(),
                a.getSizeBytes(),
                a.getStorageKey(),
                a.getUploadedBy(),
                a.getUploadedAt()
        )).toList();

        List<ExpenseApprovalDto> approvalDtos = expenseApprovalRepository.findByExpenseIdOrderByStepAsc(exp.getId())
                .stream().map(app -> new ExpenseApprovalDto(
                        app.getStep(),
                        app.getApproverName(),
                        app.getDecision().name(),
                        app.getCreatedAt(),
                        app.getComment()
                )).toList();

        return new ExpenseDto(
                exp.getId(),
                exp.getCompanyId(),
                exp.getExpenseNo(),
                exp.getExpenseDate(),
                exp.getPaidBy(),
                exp.getEmployeeUserId(),
                employeeName,
                exp.getVendorId(),
                vendorName,
                exp.getPayeeName(),
                exp.getBankAccountId(),
                bankAccountName,
                exp.getCurrencyCode(),
                exp.getExchangeRate(),
                exp.getSubtotal(),
                exp.getTaxTotal(),
                exp.getTotal(),
                exp.getStatus(),
                exp.getSubmittedAt(),
                exp.getApprovedBy(),
                approverName,
                exp.getApprovedAt(),
                exp.getRejectionReason(),
                exp.getJournalEntryId(),
                journalEntryNo,
                exp.getReimbursementEntryId(),
                reimbursementEntryNo,
                lineDtos,
                attachmentDtos,
                approvalDtos
        );
    }
}
