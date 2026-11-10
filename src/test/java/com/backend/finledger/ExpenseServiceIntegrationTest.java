package com.backend.finledger;

import com.backend.finledger.common.BusinessException;
import com.backend.finledger.common.DocType;
import com.backend.finledger.common.ForbiddenException;
import com.backend.finledger.common.NumberSeries;
import com.backend.finledger.common.NumberSeriesRepository;
import com.backend.finledger.company.entity.Company;
import com.backend.finledger.company.repository.CompanyRepository;
import com.backend.finledger.expense.dto.CreateExpenseRequest;
import com.backend.finledger.expense.dto.ExpenseCategoryDto;
import com.backend.finledger.expense.dto.ExpenseCategoryRequest;
import com.backend.finledger.expense.dto.ExpenseDto;
import com.backend.finledger.expense.dto.ExpenseLineRequest;
import com.backend.finledger.expense.entity.ExpenseStatus;
import com.backend.finledger.expense.entity.PaidBy;
import com.backend.finledger.expense.service.ExpenseCategoryService;
import com.backend.finledger.expense.service.ExpenseService;
import com.backend.finledger.ledger.entity.Account;
import com.backend.finledger.ledger.entity.AccountType;
import com.backend.finledger.ledger.entity.NormalBalance;
import com.backend.finledger.ledger.repository.AccountRepository;
import com.backend.finledger.security.UserPrincipal;
import com.backend.finledger.user.entity.User;
import com.backend.finledger.user.entity.UserStatus;
import com.backend.finledger.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ExpenseService Integration Tests (Self-Approval Blocking & Policy Limits)")
class ExpenseServiceIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private ExpenseService expenseService;

    @Autowired
    private ExpenseCategoryService categoryService;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NumberSeriesRepository numberSeriesRepository;

    private Long companyId;
    private Long expenseGlAccountId;
    private Long claimantUserId;
    private Long managerUserId;

    @BeforeEach
    void setUp() {
        Company company = companyRepository.findByCode("BSB").orElseGet(() -> {
            Company c = new Company();
            c.setCode("BSB");
            c.setLegalName("Blue Sky Business LLC");
            c.setCountryCode("AE");
            c.setBaseCurrency("AED");
            c.setFiscalYearStartMonth((byte) 1);
            return companyRepository.save(c);
        });
        companyId = company.getId();

        Account expAcc = accountRepository.findByCompanyIdAndCode(companyId, "6100").orElseGet(() -> {
            Account a = new Account(companyId, "6100", "Travel & Entertainment Expense", AccountType.EXPENSE, "EXPENSE", null, false, NormalBalance.DEBIT);
            a.setIsActive(true);
            return accountRepository.save(a);
        });
        expenseGlAccountId = expAcc.getId();

        ensureNumberSeries(companyId, DocType.EXPENSE, "EXP-{YYYY}-", 1);
        ensureNumberSeries(companyId, DocType.JOURNAL, "JV-{YYYY}-", 1);

        claimantUserId = getOrCreateUser("claimant.test@finledger.com", "Test Claimant").getId();
        managerUserId = getOrCreateUser("manager.test@finledger.com", "Test Manager").getId();
    }

    private User getOrCreateUser(String email, String fullName) {
        return userRepository.findByEmail(email).orElseGet(() -> {
            User u = new User();
            u.setCompanyId(companyId);
            u.setEmail(email);
            u.setPasswordHash("$2a$12$e8Y4J/1Q2B4O3K2J5C9Mqu");
            u.setFullName(fullName);
            u.setStatus(UserStatus.ACTIVE);
            return userRepository.save(u);
        });
    }

    private void ensureNumberSeries(Long compId, DocType docType, String prefix, Integer nextVal) {
        if (numberSeriesRepository.findByCompanyIdAndDocType(compId, docType).isEmpty()) {
            NumberSeries ns = new NumberSeries();
            ns.setCompanyId(compId);
            ns.setDocType(docType);
            ns.setPrefix(prefix);
            ns.setNextNumber(nextVal);
            ns.setPadding((byte) 4);
            numberSeriesRepository.save(ns);
        }
    }

    private void authenticateAs(Long userId, String email, String fullName, Set<String> perms) {
        UserPrincipal principal = new UserPrincipal(userId, companyId, email, "", fullName, true, perms);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );
    }

    @Test
    @Transactional
    @DisplayName("1. Block Self-Approval: Claimant attempting to approve their own claim throws SELF_APPROVAL_NOT_ALLOWED")
    void testSelfApprovalBlocking() {
        // Authenticate as claimant to create category and claim
        authenticateAs(claimantUserId, "claimant.test@finledger.com", "Test Claimant",
                Set.of("EXPENSE_VIEW", "EXPENSE_CREATE", "EXPENSE_APPROVE"));

        ExpenseCategoryDto category = categoryService.createCategory(new ExpenseCategoryRequest(
                "Supplies-" + UUID.randomUUID().toString().substring(0, 4),
                expenseGlAccountId,
                null,
                null,
                false,
                true
        ));

        ExpenseDto draft = expenseService.createExpense(new CreateExpenseRequest(
                LocalDate.now(),
                PaidBy.EMPLOYEE,
                claimantUserId,
                null,
                "Office Depot",
                null,
                "AED",
                BigDecimal.ONE,
                List.of(new ExpenseLineRequest((short) 1, category.id(), expenseGlAccountId,
                        "Printer Ink", new BigDecimal("150.00"), null, null, null, false))
        ));

        expenseService.submitExpense(draft.id());

        // Claimant tries to approve their own claim -> MUST FAIL with 403 SELF_APPROVAL_NOT_ALLOWED
        ForbiddenException ex = assertThrows(ForbiddenException.class, () ->
                expenseService.approveExpense(draft.id(), "Self-approving my claim"));
        assertEquals("SELF_APPROVAL_NOT_ALLOWED", ex.getCode(), "Self-approval must throw SELF_APPROVAL_NOT_ALLOWED");

        // Now manager approves -> succeeds
        authenticateAs(managerUserId, "manager.test@finledger.com", "Test Manager",
                Set.of("EXPENSE_VIEW", "EXPENSE_APPROVE", "JOURNAL_POST"));
        ExpenseDto approved = expenseService.approveExpense(draft.id(), "Approved by manager");
        assertEquals(ExpenseStatus.POSTED, approved.status(), "Single approval <= 2000 AED posts directly");
    }

    @Test
    @Transactional
    @DisplayName("2. Per-Claim Limit Enforcement: Claim line exceeding category limitPerClaim throws POLICY_LIMIT_EXCEEDED")
    void testPerClaimPolicyLimitEnforcement() {
        authenticateAs(claimantUserId, "claimant.test@finledger.com", "Test Claimant",
                Set.of("EXPENSE_VIEW", "EXPENSE_CREATE", "EXPENSE_APPROVE"));

        // Create category with per-claim limit = 200.00 AED
        ExpenseCategoryDto category = categoryService.createCategory(new ExpenseCategoryRequest(
                "Meal-" + UUID.randomUUID().toString().substring(0, 4),
                expenseGlAccountId,
                null,
                new BigDecimal("200.00"),
                false,
                true
        ));

        // Create claim with line amount = 350.00 AED (> 200.00 AED limit)
        ExpenseDto draft = expenseService.createExpense(new CreateExpenseRequest(
                LocalDate.now(),
                PaidBy.EMPLOYEE,
                claimantUserId,
                null,
                "Gourmet Restaurant",
                null,
                "AED",
                BigDecimal.ONE,
                List.of(new ExpenseLineRequest((short) 1, category.id(), expenseGlAccountId,
                        "Dinner with client", new BigDecimal("350.00"), null, null, null, false))
        ));

        // Submission must be blocked with POLICY_LIMIT_EXCEEDED
        BusinessException ex = assertThrows(BusinessException.class, () ->
                expenseService.submitExpense(draft.id()));
        assertEquals("POLICY_LIMIT_EXCEEDED", ex.getCode(), "Claim exceeding limit must throw POLICY_LIMIT_EXCEEDED");
    }
}
