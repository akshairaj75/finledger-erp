package com.backend.finledger.expense.service.impl;

import com.backend.finledger.banking.entity.BankAccount;
import com.backend.finledger.banking.repository.BankAccountRepository;
import com.backend.finledger.common.BusinessException;
import com.backend.finledger.company.entity.Company;
import com.backend.finledger.company.repository.CompanyRepository;
import com.backend.finledger.expense.dto.BatchReimburseRequest;
import com.backend.finledger.expense.dto.BatchReimburseResponse;
import com.backend.finledger.expense.entity.Expense;
import com.backend.finledger.expense.entity.ExpenseStatus;
import com.backend.finledger.expense.entity.PaidBy;
import com.backend.finledger.expense.repository.ExpenseRepository;
import com.backend.finledger.expense.service.ReimbursementService;
import com.backend.finledger.ledger.dto.JournalEntryDto;
import com.backend.finledger.ledger.dto.JournalLineRequest;
import com.backend.finledger.ledger.dto.SourceDocumentPostingRequest;
import com.backend.finledger.ledger.entity.Account;
import com.backend.finledger.ledger.entity.AccountType;
import com.backend.finledger.ledger.entity.JournalSourceType;
import com.backend.finledger.ledger.entity.NormalBalance;
import com.backend.finledger.ledger.repository.AccountRepository;
import com.backend.finledger.ledger.service.PostingService;
import com.backend.finledger.security.CurrentUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@Service
public class ReimbursementServiceImpl implements ReimbursementService {

    private final ExpenseRepository expenseRepository;
    private final BankAccountRepository bankAccountRepository;
    private final AccountRepository accountRepository;
    private final CompanyRepository companyRepository;
    private final PostingService postingService;

    public ReimbursementServiceImpl(
            ExpenseRepository expenseRepository,
            BankAccountRepository bankAccountRepository,
            AccountRepository accountRepository,
            CompanyRepository companyRepository,
            PostingService postingService) {
        this.expenseRepository = expenseRepository;
        this.bankAccountRepository = bankAccountRepository;
        this.accountRepository = accountRepository;
        this.companyRepository = companyRepository;
        this.postingService = postingService;
    }

    @Override
    @Transactional
    public BatchReimburseResponse reimburseExpenses(BatchReimburseRequest request) {
        Long companyId = CurrentUser.getCompanyId();

        if (request.expenseIds() == null || request.expenseIds().isEmpty()) {
            throw new BusinessException("EMPTY_EXPENSES", "At least one expense must be selected for reimbursement.");
        }

        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new BusinessException("COMPANY_NOT_FOUND", "Company not found: " + companyId));

        BankAccount bankAccount = bankAccountRepository.findByCompanyIdAndId(companyId, request.bankAccountId())
                .orElseThrow(() -> new BusinessException("BANK_ACCOUNT_NOT_FOUND", "Bank account not found: " + request.bankAccountId()));

        List<Expense> expenses = expenseRepository.findByCompanyIdAndIdIn(companyId, request.expenseIds());
        if (expenses.size() != request.expenseIds().size()) {
            throw new BusinessException("EXPENSE_NOT_FOUND", "One or more requested expenses were not found for company " + companyId);
        }

        BigDecimal totalAmount = BigDecimal.ZERO;
        List<String> expenseNumbers = new ArrayList<>();
        List<Long> settledIds = new ArrayList<>();

        for (Expense exp : expenses) {
            if (exp.getPaidBy() != PaidBy.EMPLOYEE) {
                throw new BusinessException("INVALID_PAYEE_TYPE", "Expense " + exp.getExpenseNo() + " is company-paid and cannot be reimbursed.");
            }
            if (exp.getStatus() != ExpenseStatus.POSTED && exp.getStatus() != ExpenseStatus.APPROVED) {
                throw new BusinessException("INVALID_EXPENSE_STATUS",
                        "Expense " + exp.getExpenseNo() + " cannot be reimbursed in status: " + exp.getStatus());
            }

            totalAmount = totalAmount.add(exp.getTotal());
            expenseNumbers.add(exp.getExpenseNo());
            settledIds.add(exp.getId());
        }

        Long reimbursementsPayableAccountId = getOrCreateEmployeeReimbursementsAccount(companyId);

        List<JournalLineRequest> journalLines = List.of(
                // Dr Employee Reimbursements Payable
                new JournalLineRequest(
                        reimbursementsPayableAccountId,
                        null,
                        "Employee Reimbursements Settlement (" + expenseNumbers.size() + " claims)",
                        totalAmount,
                        BigDecimal.ZERO,
                        null,
                        null
                ),
                // Cr Bank / Cash
                new JournalLineRequest(
                        bankAccount.getGlAccountId(),
                        null,
                        "Reimbursement payment via " + bankAccount.getName(),
                        BigDecimal.ZERO,
                        totalAmount,
                        null,
                        null
                )
        );

        LocalDate entryDate = request.paymentDate() != null ? request.paymentDate() : LocalDate.now();
        String reference = "REIMB-" + String.join(",", expenseNumbers);
        if (reference.length() > 60) {
            reference = reference.substring(0, 57) + "...";
        }

        SourceDocumentPostingRequest postingRequest = new SourceDocumentPostingRequest(
                JournalSourceType.REIMBURSEMENT,
                expenses.get(0).getId(),
                entryDate,
                reference,
                "Batch Reimbursement for: " + String.join(", ", expenseNumbers),
                company.getBaseCurrency(),
                BigDecimal.ONE,
                journalLines
        );

        JournalEntryDto postedEntry = postingService.postDocument(postingRequest);

        for (Expense exp : expenses) {
            exp.setStatus(ExpenseStatus.REIMBURSED);
            exp.setReimbursementEntryId(postedEntry.id());
            expenseRepository.save(exp);
        }

        return new BatchReimburseResponse(
                postedEntry.id(),
                postedEntry.entryNo(),
                totalAmount,
                expenses.size(),
                settledIds
        );
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
}

