package com.backend.finledger;

import com.backend.finledger.attachment.dto.AttachmentDto;
import com.backend.finledger.attachment.repository.AttachmentRepository;
import com.backend.finledger.attachment.service.AttachmentService;
import com.backend.finledger.banking.entity.AccountKind;
import com.backend.finledger.banking.entity.BankAccount;
import com.backend.finledger.banking.repository.BankAccountRepository;
import com.backend.finledger.common.BusinessException;
import com.backend.finledger.common.DocType;
import com.backend.finledger.common.ForbiddenException;
import com.backend.finledger.common.NumberSeries;
import com.backend.finledger.common.NumberSeriesRepository;
import com.backend.finledger.company.entity.Company;
import com.backend.finledger.company.repository.CompanyRepository;
import com.backend.finledger.expense.dto.*;
import com.backend.finledger.expense.entity.Expense;
import com.backend.finledger.expense.entity.ExpenseStatus;
import com.backend.finledger.expense.entity.PaidBy;
import com.backend.finledger.expense.repository.ExpenseApprovalRepository;
import com.backend.finledger.expense.repository.ExpenseCategoryRepository;
import com.backend.finledger.expense.repository.ExpenseRepository;
import com.backend.finledger.expense.service.ExpenseCategoryService;
import com.backend.finledger.expense.service.ExpenseService;
import com.backend.finledger.expense.service.ReimbursementService;
import com.backend.finledger.ledger.dto.CreateFiscalYearRequest;
import com.backend.finledger.ledger.dto.FiscalYearDto;
import com.backend.finledger.ledger.entity.*;
import com.backend.finledger.ledger.repository.AccountRepository;
import com.backend.finledger.ledger.repository.FiscalYearRepository;
import com.backend.finledger.ledger.repository.JournalEntryRepository;
import com.backend.finledger.ledger.service.FiscalYearPeriodService;
import com.backend.finledger.security.UserPrincipal;
import com.backend.finledger.tax.entity.TaxRate;
import com.backend.finledger.tax.entity.TaxType;
import com.backend.finledger.tax.repository.TaxRateRepository;
import com.backend.finledger.user.entity.User;
import com.backend.finledger.user.entity.UserStatus;
import com.backend.finledger.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class ExpenseAndAttachmentTest {

    @Autowired
    private ExpenseService expenseService;

    @Autowired
    private ExpenseCategoryService categoryService;

    @Autowired
    private ReimbursementService reimbursementService;

    @Autowired
    private AttachmentService attachmentService;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TaxRateRepository taxRateRepository;

    @Autowired
    private BankAccountRepository bankAccountRepository;

    @Autowired
    private NumberSeriesRepository numberSeriesRepository;

    @Autowired
    private FiscalYearRepository fiscalYearRepository;

    @Autowired
    private FiscalYearPeriodService fiscalYearPeriodService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ExpenseRepository expenseRepository;

    @Autowired
    private ExpenseApprovalRepository expenseApprovalRepository;

    @Autowired
    private ExpenseCategoryRepository expenseCategoryRepository;

    @Autowired
    private AttachmentRepository attachmentRepository;

    @Autowired
    private JournalEntryRepository journalEntryRepository;

    private Long companyId;
    private Long expenseGlAccountId;
    private Long vatInputAccountId;
    private Long bankGlAccountId;
    private Long bankAccountId;
    private Long taxRate5Id;
    private Long claimantUserId;
    private Long managerUserId;
    private Long financeUserId;

    private LocalDate today;

    @BeforeEach
    void setUp() {
        today = LocalDate.now();

        Company company = companyRepository.findByCode("BSB").orElseGet(() -> {
            Company c = new Company();
            c.setCode("BSB");
            c.setLegalName("BSB Trading LLC");
            c.setCountryCode("AE");
            c.setBaseCurrency("AED");
            c.setFiscalYearStartMonth((byte) 1);
            c.setIsActive(true);
            return companyRepository.save(c);
        });
        companyId = company.getId();

        // 1. Fiscal year & open periods
        int currentYear = today.getYear();
        String fyName = "FY" + currentYear;
        fiscalYearRepository.findByCompanyIdAndName(companyId, fyName).orElseGet(() -> {
            CreateFiscalYearRequest fyReq = new CreateFiscalYearRequest(
                    fyName,
                    LocalDate.of(currentYear, 1, 1),
                    LocalDate.of(currentYear, 12, 31),
                    true
            );
            FiscalYearDto dto = fiscalYearPeriodService.createFiscalYear(fyReq);
            return fiscalYearRepository.findById(dto.id()).orElseThrow();
        });

        // 2. Chart of Accounts
        Account expAcc = accountRepository.findByCompanyIdAndCode(companyId, "5250").orElseGet(() -> {
            Account a = new Account(companyId, "5250", "Travel & Entertainment Expense", AccountType.EXPENSE, "EXPENSE", null, false, NormalBalance.DEBIT);
            a.setIsActive(true);
            return accountRepository.save(a);
        });
        expenseGlAccountId = expAcc.getId();

        Account vatIn = accountRepository.findByCompanyIdAndCode(companyId, "1400").orElseGet(() -> {
            Account a = new Account(companyId, "1400", "Input VAT (Recoverable)", AccountType.ASSET, "TAX", null, false, NormalBalance.DEBIT);
            a.setIsActive(true);
            return accountRepository.save(a);
        });
        vatInputAccountId = vatIn.getId();

        Account bankGl = accountRepository.findByCompanyIdAndCode(companyId, "1110").orElseGet(() -> {
            Account a = new Account(companyId, "1110", "Emirates NBD - Current", AccountType.ASSET, "BANK", null, false, NormalBalance.DEBIT);
            a.setIsActive(true);
            return accountRepository.save(a);
        });
        bankGlAccountId = bankGl.getId();

        // Reimbursements Payable liability account
        accountRepository.findByCompanyIdAndCode(companyId, "2150").orElseGet(() -> {
            Account a = new Account(companyId, "2150", "Employee Reimbursements Payable", AccountType.LIABILITY, "PAYABLE", null, false, NormalBalance.CREDIT);
            a.setIsActive(true);
            a.setIsSystem(true);
            return accountRepository.save(a);
        });

        // 3. Tax Rate
        TaxRate taxRate = taxRateRepository.findByCompanyIdAndCode(companyId, "VAT5-EXP").orElseGet(() -> {
            TaxRate tr = new TaxRate(companyId, "VAT5-EXP", "5% VAT on Expenses", new BigDecimal("5.0000"), TaxType.STANDARD);
            tr.setPurchaseAccountId(vatInputAccountId);
            return taxRateRepository.save(tr);
        });
        taxRate.setPurchaseAccountId(vatInputAccountId);
        taxRateRepository.save(taxRate);
        taxRate5Id = taxRate.getId();

        // 4. Bank Account
        BankAccount bank = bankAccountRepository.findByCompanyIdAndIsActiveTrue(companyId).stream()
                .filter(b -> b.getGlAccountId().equals(bankGlAccountId))
                .findFirst()
                .orElseGet(() -> {
                    BankAccount ba = new BankAccount();
                    ba.setCompanyId(companyId);
                    ba.setName("Company Operations Account");
                    ba.setBankName("Emirates NBD");
                    ba.setAccountNumber("1234567890");
                    ba.setIban("AE123456789012345678901");
                    ba.setCurrencyCode("AED");
                    ba.setGlAccountId(bankGlAccountId);
                    ba.setAccountKind(AccountKind.BANK);
                    ba.setIsActive(true);
                    return bankAccountRepository.save(ba);
                });
        bankAccountId = bank.getId();

        // 5. Test Users: Claimant (Sara), Manager (Raj), Finance (Alex)
        claimantUserId = getOrCreateUser("sara.mathew@bsbtrading.ae", "Sara Mathew").getId();
        managerUserId = getOrCreateUser("raj.patel@bsbtrading.ae", "Raj Patel").getId();
        financeUserId = getOrCreateUser("alex.finance@bsbtrading.ae", "Alex Finance").getId();

        // 6. Number Series for EXPENSE and JOURNAL
        ensureNumberSeries(companyId, DocType.EXPENSE, "EXP-{YYYY}-", 1);
        ensureNumberSeries(companyId, DocType.JOURNAL, "JV-{YYYY}-", 1);

        // Default login as admin
        authenticateAs(1L, companyId, "admin@finledger.com", "Admin User", Set.of(
                "SETTINGS_EDIT", "EXPENSE_VIEW", "EXPENSE_CREATE", "EXPENSE_APPROVE", "EXPENSE_REIMBURSE", "JOURNAL_VIEW", "JOURNAL_POST"
        ));
    }

    private User getOrCreateUser(String email, String fullName) {
        return userRepository.findByEmail(email).orElseGet(() -> {
            User u = new User();
            u.setCompanyId(companyId);
            u.setEmail(email);
            u.setPasswordHash("$2a$12$dummyHashPlaceholderForTestsOnly0000000000000000000000");
            u.setFullName(fullName);
            u.setStatus(UserStatus.ACTIVE);
            return userRepository.save(u);
        });
    }

    private void ensureNumberSeries(Long compId, DocType docType, String prefix, Integer nextVal) {
        var opt = numberSeriesRepository.findByCompanyIdAndDocType(compId, docType);
        if (opt.isEmpty()) {
            NumberSeries ns = new NumberSeries();
            ns.setCompanyId(compId);
            ns.setDocType(docType);
            ns.setPrefix(prefix);
            ns.setNextNumber(nextVal);
            ns.setPadding((byte) 4);
            numberSeriesRepository.save(ns);
        }
    }

    private void authenticateAs(Long userId, Long compId, String email, String fullName, Set<String> permissions) {
        UserPrincipal principal = new UserPrincipal(userId, compId, email, "", fullName, true, permissions);
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    @Transactional
    @DisplayName("1. Attachment Service: Upload JPG/PNG/PDF, load resource, reject invalid types & size limits, lock on non-draft")
    void testAttachmentStorageAndPolicy() throws Exception {
        // Create draft expense
        ExpenseDto draftExpense = expenseService.createExpense(new CreateExpenseRequest(
                today,
                PaidBy.EMPLOYEE,
                claimantUserId,
                null,
                "Taxi Dubai",
                null,
                "AED",
                BigDecimal.ONE,
                List.of(new ExpenseLineRequest((short) 1, createCategory("Local Transport", null, false).id(),
                        expenseGlAccountId, "Taxi ride", new BigDecimal("50.00"), null, null, null, false))
        ));

        // 1. Upload valid PDF receipt
        MockMultipartFile pdfFile = new MockMultipartFile(
                "file",
                "receipt_taxi.pdf",
                "application/pdf",
                "%PDF-1.4 sample taxi invoice receipt content".getBytes(StandardCharsets.UTF_8)
        );

        AttachmentDto attDto = attachmentService.uploadAttachment("EXPENSE", draftExpense.id(), pdfFile);
        assertNotNull(attDto);
        assertNotNull(attDto.id());
        assertEquals("receipt_taxi.pdf", attDto.fileName());
        assertEquals("application/pdf", attDto.contentType());
        assertTrue(attDto.sizeBytes() > 0);
        assertNotNull(attDto.storageKey());

        // 2. Load resource
        Resource res = attachmentService.downloadAttachment(attDto.id());
        assertTrue(res.exists());
        assertTrue(res.isReadable());

        // 3. Reject invalid extension/content type
        MockMultipartFile exeFile = new MockMultipartFile(
                "file",
                "malicious.exe",
                "application/octet-stream",
                "malware bytes".getBytes(StandardCharsets.UTF_8)
        );
        BusinessException exType = assertThrows(BusinessException.class, () ->
                attachmentService.uploadAttachment("EXPENSE", draftExpense.id(), exeFile));
        assertEquals("INVALID_FILE_TYPE", exType.getCode());

        // 4. Delete attachment on draft document succeeds
        attachmentService.deleteAttachment(attDto.id());
        assertEquals(0, attachmentService.countAttachments("EXPENSE", draftExpense.id()));
    }

    @Test
    @Transactional
    @DisplayName("2. Receipt Requirement Policy Enforcement upon Submission (HTTP 422 RECEIPT_REQUIRED)")
    void testReceiptRequirementEnforcementOnSubmission() {
        // Create category with receipt_required = true
        ExpenseCategoryDto category = createCategory("Client Meals", null, true);

        // Create draft expense without attachment
        ExpenseDto draftExpense = expenseService.createExpense(new CreateExpenseRequest(
                today,
                PaidBy.EMPLOYEE,
                claimantUserId,
                null,
                "Business Lunch",
                null,
                "AED",
                BigDecimal.ONE,
                List.of(new ExpenseLineRequest((short) 1, category.id(), expenseGlAccountId,
                        "Dinner with client", new BigDecimal("350.00"), null, null, null, false))
        ));

        // Submission must fail with RECEIPT_REQUIRED because no attachment is uploaded
        BusinessException ex = assertThrows(BusinessException.class, () ->
                expenseService.submitExpense(draftExpense.id()));
        assertEquals("RECEIPT_REQUIRED", ex.getCode());

        // Now upload a PNG receipt
        MockMultipartFile receiptImage = new MockMultipartFile(
                "file",
                "lunch_receipt.png",
                "image/png",
                new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A}
        );
        attachmentService.uploadAttachment("EXPENSE", draftExpense.id(), receiptImage);

        // Now submission must succeed
        ExpenseDto submitted = expenseService.submitExpense(draftExpense.id());
        assertEquals(ExpenseStatus.SUBMITTED, submitted.status());
        assertNotNull(submitted.submittedAt());
    }

    @Test
    @Transactional
    @DisplayName("3. Policy Limit Per Claim Enforcement upon Submission (HTTP 422 POLICY_LIMIT_EXCEEDED)")
    void testPolicyLimitPerClaimEnforcement() {
        // Category with per-claim limit of AED 300.00
        ExpenseCategoryDto category = createCategory("Mobile Phone Allowance", new BigDecimal("300.00"), false);

        // Claim exceeds the per-claim limit (AED 450.00)
        ExpenseDto draftExpense = expenseService.createExpense(new CreateExpenseRequest(
                today,
                PaidBy.EMPLOYEE,
                claimantUserId,
                null,
                "Telecom Provider",
                null,
                "AED",
                BigDecimal.ONE,
                List.of(new ExpenseLineRequest((short) 1, category.id(), expenseGlAccountId,
                        "Monthly phone bill", new BigDecimal("450.00"), null, null, null, false))
        ));

        // Submission must fail with POLICY_LIMIT_EXCEEDED
        BusinessException ex = assertThrows(BusinessException.class, () ->
                expenseService.submitExpense(draftExpense.id()));
        assertEquals("POLICY_LIMIT_EXCEEDED", ex.getCode());
    }

    @Test
    @Transactional
    @DisplayName("4. Block Self-Approval: Claimant cannot approve their own claim (HTTP 403 SELF_APPROVAL_NOT_ALLOWED)")
    void testSelfApprovalBlock() {
        ExpenseCategoryDto category = createCategory("Office Supplies", null, false);

        // Claimant creates and submits claim
        authenticateAs(claimantUserId, companyId, "sara.mathew@bsbtrading.ae", "Sara Mathew",
                Set.of("EXPENSE_VIEW", "EXPENSE_CREATE", "EXPENSE_APPROVE"));

        ExpenseDto draft = expenseService.createExpense(new CreateExpenseRequest(
                today,
                PaidBy.EMPLOYEE,
                claimantUserId,
                null,
                "Stationery Shop",
                null,
                "AED",
                BigDecimal.ONE,
                List.of(new ExpenseLineRequest((short) 1, category.id(), expenseGlAccountId,
                        "Notebooks & Pens", new BigDecimal("120.00"), null, null, null, false))
        ));
        expenseService.submitExpense(draft.id());

        // Claimant attempts to self-approve
        ForbiddenException ex = assertThrows(ForbiddenException.class, () ->
                expenseService.approveExpense(draft.id(), "Approving my own claim"));
        assertEquals("SELF_APPROVAL_NOT_ALLOWED", ex.getCode());
    }

    @Test
    @Transactional
    @DisplayName("5. Approval Chain & Posting: Claims <= 2,000 AED require 1 Step and post immediately to ledger")
    void testSingleStepApprovalAndPosting() {
        ExpenseCategoryDto category = createCategory("Client Entertainment", null, false);

        authenticateAs(claimantUserId, companyId, "sara.mathew@bsbtrading.ae", "Sara Mathew",
                Set.of("EXPENSE_VIEW", "EXPENSE_CREATE"));

        ExpenseDto draft = expenseService.createExpense(new CreateExpenseRequest(
                today,
                PaidBy.EMPLOYEE,
                claimantUserId,
                null,
                "Downtown Cafe",
                null,
                "AED",
                BigDecimal.ONE,
                List.of(new ExpenseLineRequest((short) 1, category.id(), expenseGlAccountId,
                        "Client coffee meetings", new BigDecimal("831.00"), taxRate5Id, null, null, false))
        ));
        expenseService.submitExpense(draft.id());

        // Line manager (Raj) approves claim
        authenticateAs(managerUserId, companyId, "raj.patel@bsbtrading.ae", "Raj Patel",
                Set.of("EXPENSE_VIEW", "EXPENSE_APPROVE", "JOURNAL_POST"));

        ExpenseDto approved = expenseService.approveExpense(draft.id(), "OK - client visits approved");

        assertEquals(ExpenseStatus.POSTED, approved.status());
        assertNotNull(approved.approvedBy());
        assertNotNull(approved.approvedAt());
        assertNotNull(approved.journalEntryId());
        assertNotNull(approved.journalEntryNo());
        assertEquals(1, approved.approvals().size());
        assertEquals("APPROVED", approved.approvals().get(0).decision());
        assertEquals("Raj Patel", approved.approvals().get(0).approver());

        // Verify Journal Entry created
        JournalEntry entry = journalEntryRepository.findById(approved.journalEntryId()).orElseThrow();
        assertEquals(JournalStatus.POSTED, entry.getStatus());
        assertEquals(JournalSourceType.EXPENSE, entry.getSourceType());
        assertEquals(draft.id(), entry.getSourceId());

        // Dr Expense (831.00) + Dr Input VAT (41.55) = Cr Reimbursements Payable (872.55)
        assertEquals(entry.getTotalDebit(), entry.getTotalCredit());
        assertEquals(new BigDecimal("872.55"), entry.getTotalDebit());
    }

    @Test
    @Transactional
    @DisplayName("6. Approval Chain: Claims > 2,000 AED require Line Manager (Step 1) + Finance Review (Step 2)")
    void testTwoStepApprovalChainForLargeClaims() {
        ExpenseCategoryDto category = createCategory("Overseas Flight & Hotel", null, false);

        authenticateAs(claimantUserId, companyId, "sara.mathew@bsbtrading.ae", "Sara Mathew",
                Set.of("EXPENSE_VIEW", "EXPENSE_CREATE"));

        // Claim total: 3,500 AED (> 2,000 AED threshold)
        ExpenseDto draft = expenseService.createExpense(new CreateExpenseRequest(
                today,
                PaidBy.EMPLOYEE,
                claimantUserId,
                null,
                "Emirates Airlines",
                null,
                "AED",
                BigDecimal.ONE,
                List.of(new ExpenseLineRequest((short) 1, category.id(), expenseGlAccountId,
                        "Flight tickets to Riyadh summit", new BigDecimal("3500.00"), null, null, null, false))
        ));
        expenseService.submitExpense(draft.id());

        // Step 1: Line Manager (Raj) approves
        authenticateAs(managerUserId, companyId, "raj.patel@bsbtrading.ae", "Raj Patel",
                Set.of("EXPENSE_VIEW", "EXPENSE_APPROVE"));

        ExpenseDto step1Dto = expenseService.approveExpense(draft.id(), "Manager approved - pending finance review");
        // Status remains SUBMITTED until final approval!
        assertEquals(ExpenseStatus.SUBMITTED, step1Dto.status());
        assertNull(step1Dto.journalEntryId());
        assertEquals(1, step1Dto.approvals().size());
        assertEquals(1, step1Dto.approvals().get(0).step());

        // Step 2: Finance Reviewer (Alex) approves
        authenticateAs(financeUserId, companyId, "alex.finance@bsbtrading.ae", "Alex Finance",
                Set.of("EXPENSE_VIEW", "EXPENSE_APPROVE", "JOURNAL_POST"));

        ExpenseDto step2Dto = expenseService.approveExpense(draft.id(), "Finance verified budget");
        // Final approval reached -> Status POSTED and Journal Entry created!
        assertEquals(ExpenseStatus.POSTED, step2Dto.status());
        assertNotNull(step2Dto.journalEntryId());
        assertEquals(2, step2Dto.approvals().size());
        assertEquals(2, step2Dto.approvals().get(1).step());
        assertEquals("Alex Finance", step2Dto.approvals().get(1).approver());
    }

    @Test
    @Transactional
    @DisplayName("7. Company-Paid Expenses: Post immediately upon save (Dr Expense + Dr VAT, Cr Bank)")
    void testCompanyPaidExpensePostingImmediately() {
        ExpenseCategoryDto category = createCategory("Software Subscriptions", null, false);

        authenticateAs(1L, companyId, "admin@finledger.com", "Admin User",
                Set.of("EXPENSE_VIEW", "EXPENSE_CREATE", "JOURNAL_POST"));

        ExpenseDto companyExp = expenseService.createExpense(new CreateExpenseRequest(
                today,
                PaidBy.COMPANY,
                null,
                null,
                "GitHub / JetBrains",
                bankAccountId,
                "AED",
                BigDecimal.ONE,
                List.of(new ExpenseLineRequest((short) 1, category.id(), expenseGlAccountId,
                        "Annual IDE Licenses", new BigDecimal("1200.00"), taxRate5Id, null, null, false))
        ));

        assertEquals(ExpenseStatus.POSTED, companyExp.status());
        assertNotNull(companyExp.journalEntryId());

        JournalEntry je = journalEntryRepository.findById(companyExp.journalEntryId()).orElseThrow();
        assertEquals(JournalStatus.POSTED, je.getStatus());
        assertEquals(new BigDecimal("1260.00"), je.getTotalDebit());
        assertEquals(new BigDecimal("1260.00"), je.getTotalCredit());

        // Check credit line is to bank account GL
        boolean hasBankCredit = je.getLines().stream().anyMatch(jl ->
                jl.getAccountId().equals(bankGlAccountId) && jl.getCredit().compareTo(new BigDecimal("1260.00")) == 0);
        assertTrue(hasBankCredit);
    }

    @Test
    @Transactional
    @DisplayName("8. Batch Reimbursement: Settles multiple approved claims (Dr Employee Reimbursements Payable, Cr Bank)")
    void testBatchReimbursement() {
        ExpenseCategoryDto category = createCategory("General Field Expenses", null, false);

        authenticateAs(claimantUserId, companyId, "sara.mathew@bsbtrading.ae", "Sara Mathew",
                Set.of("EXPENSE_VIEW", "EXPENSE_CREATE"));

        // Create Claim 1: 500 AED
        ExpenseDto c1 = expenseService.createExpense(new CreateExpenseRequest(
                today, PaidBy.EMPLOYEE, claimantUserId, null, "Vendor 1", null, "AED", BigDecimal.ONE,
                List.of(new ExpenseLineRequest((short) 1, category.id(), expenseGlAccountId, "Site visit 1", new BigDecimal("500.00"), null, null, null, false))
        ));
        expenseService.submitExpense(c1.id());

        // Create Claim 2: 700 AED
        ExpenseDto c2 = expenseService.createExpense(new CreateExpenseRequest(
                today, PaidBy.EMPLOYEE, claimantUserId, null, "Vendor 2", null, "AED", BigDecimal.ONE,
                List.of(new ExpenseLineRequest((short) 1, category.id(), expenseGlAccountId, "Site visit 2", new BigDecimal("700.00"), null, null, null, false))
        ));
        expenseService.submitExpense(c2.id());

        // Manager approves both claims
        authenticateAs(managerUserId, companyId, "raj.patel@bsbtrading.ae", "Raj Patel",
                Set.of("EXPENSE_VIEW", "EXPENSE_APPROVE", "EXPENSE_REIMBURSE", "JOURNAL_POST"));

        expenseService.approveExpense(c1.id(), "Approved 1");
        expenseService.approveExpense(c2.id(), "Approved 2");

        // Execute Batch Reimbursement for both claims (Total: 1,200 AED)
        BatchReimburseResponse reimbRes = reimbursementService.reimburseExpenses(new BatchReimburseRequest(
                List.of(c1.id(), c2.id()),
                bankAccountId,
                today
        ));

        assertNotNull(reimbRes);
        assertEquals(2, reimbRes.settledExpenseCount());
        assertEquals(new BigDecimal("1260.00"), reimbRes.totalAmount());
        assertNotNull(reimbRes.reimbursementEntryId());

        // Verify claims are now REIMBURSED
        Expense exp1 = expenseRepository.findById(c1.id()).orElseThrow();
        Expense exp2 = expenseRepository.findById(c2.id()).orElseThrow();
        assertEquals(ExpenseStatus.REIMBURSED, exp1.getStatus());
        assertEquals(ExpenseStatus.REIMBURSED, exp2.getStatus());
        assertEquals(reimbRes.reimbursementEntryId(), exp1.getReimbursementEntryId());
        assertEquals(reimbRes.reimbursementEntryId(), exp2.getReimbursementEntryId());

        // Verify Journal Entry for reimbursement:
        // Dr Employee Reimbursements Payable (1,260.00)
        // Cr Bank Account (1,260.00)
        JournalEntry reimbJe = journalEntryRepository.findById(reimbRes.reimbursementEntryId()).orElseThrow();
        assertEquals(JournalStatus.POSTED, reimbJe.getStatus());
        assertEquals(JournalSourceType.REIMBURSEMENT, reimbJe.getSourceType());
        assertEquals(new BigDecimal("1260.00"), reimbJe.getTotalDebit());
        assertEquals(new BigDecimal("1260.00"), reimbJe.getTotalCredit());

        boolean hasBankCredit = reimbJe.getLines().stream().anyMatch(jl ->
                jl.getAccountId().equals(bankGlAccountId) && jl.getCredit().compareTo(new BigDecimal("1260.00")) == 0);
        assertTrue(hasBankCredit);
    }

    private ExpenseCategoryDto createCategory(String name, BigDecimal limitPerClaim, boolean receiptRequired) {
        String uniqueName = name + "_" + System.currentTimeMillis();
        return categoryService.createCategory(new ExpenseCategoryRequest(
                uniqueName,
                expenseGlAccountId,
                taxRate5Id,
                limitPerClaim,
                receiptRequired,
                true
        ));
    }
}
