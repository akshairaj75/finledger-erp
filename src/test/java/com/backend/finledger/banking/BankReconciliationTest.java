package com.backend.finledger.banking;

import com.backend.finledger.banking.dto.BankReconciliationDto;
import com.backend.finledger.banking.dto.BankReconciliationRequest;
import com.backend.finledger.banking.entity.AccountKind;
import com.backend.finledger.banking.entity.BankAccount;
import com.backend.finledger.banking.entity.BankReconciliation;
import com.backend.finledger.banking.entity.ReconciliationStatus;
import com.backend.finledger.banking.repository.BankAccountRepository;
import com.backend.finledger.banking.repository.BankReconciliationRepository;
import com.backend.finledger.banking.repository.BankStatementLineRepository;
import com.backend.finledger.banking.repository.BankStatementRepository;
import com.backend.finledger.banking.service.impl.BankReconciliationServiceImpl;
import com.backend.finledger.common.BusinessException;
import com.backend.finledger.ledger.entity.Account;
import com.backend.finledger.ledger.entity.AccountType;
import com.backend.finledger.ledger.entity.NormalBalance;
import com.backend.finledger.ledger.repository.AccountRepository;
import com.backend.finledger.ledger.repository.JournalLineRepository;
import com.backend.finledger.ledger.service.PostingService;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BankReconciliationTest {

    @Mock
    private BankReconciliationRepository reconciliationRepository;
    @Mock
    private BankAccountRepository bankAccountRepository;
    @Mock
    private BankStatementRepository statementRepository;
    @Mock
    private BankStatementLineRepository statementLineRepository;
    @Mock
    private JournalLineRepository journalLineRepository;
    @Mock
    private AccountRepository accountRepository;
    @Mock
    private PostingService postingService;

    @InjectMocks
    private BankReconciliationServiceImpl reconciliationService;

    private MockedStatic<CurrentUser> currentUserMock;
    private BankAccount testBankAccount;
    private Account testGlAccount;

    @BeforeEach
    void setUp() {
        currentUserMock = Mockito.mockStatic(CurrentUser.class);
        currentUserMock.when(CurrentUser::getCompanyId).thenReturn(1L);

        testBankAccount = new BankAccount();
        testBankAccount.setId(10L);
        testBankAccount.setName("Emirates NBD Current");
        testBankAccount.setGlAccountId(101L);
        testBankAccount.setCurrencyCode("AED");
        testBankAccount.setAccountKind(AccountKind.BANK);

        testGlAccount = new Account();
        testGlAccount.setId(101L);
        testGlAccount.setCode("1110");
        testGlAccount.setName("Bank Account - ENBD");
        testGlAccount.setAccountType(AccountType.ASSET);
        testGlAccount.setNormalBalance(NormalBalance.DEBIT);
        testGlAccount.setOpeningBalance(BigDecimal.valueOf(1000));
    }

    @AfterEach
    void tearDown() {
        currentUserMock.close();
    }

    @Test
    @DisplayName("Should start new bank reconciliation and calculate balances")
    void testStartReconciliation() {
        LocalDate stmtDate = LocalDate.of(2026, 10, 31);
        BigDecimal stmtBal = BigDecimal.valueOf(1500);

        when(bankAccountRepository.findByCompanyIdAndId(1L, 10L)).thenReturn(Optional.of(testBankAccount));
        when(accountRepository.findByCompanyIdAndId(1L, 101L)).thenReturn(Optional.of(testGlAccount));
        when(reconciliationRepository.findByBankAccountIdAndStatementDate(10L, stmtDate)).thenReturn(Optional.empty());
        when(journalLineRepository.findAllPostedLinesForAccount(1L, 101L, stmtDate)).thenReturn(Collections.emptyList());
        when(statementRepository.findByBankAccountIdOrderByStatementDateDesc(10L)).thenReturn(Collections.emptyList());
        when(journalLineRepository.findUnreconciledLinesForAccount(1L, 101L, stmtDate)).thenReturn(Collections.emptyList());

        BankReconciliationRequest req = new BankReconciliationRequest(10L, stmtDate, stmtBal);
        BankReconciliationDto result = reconciliationService.startReconciliation(req);

        assertNotNull(result);
        assertEquals(10L, result.bankAccountId());
        assertEquals(stmtBal, result.statementBalance());
        assertEquals(BigDecimal.valueOf(1000), result.bookBalance());
        assertEquals(BigDecimal.valueOf(500), result.difference());
        assertEquals(ReconciliationStatus.IN_PROGRESS, result.status());

        verify(reconciliationRepository, times(1)).save(any(BankReconciliation.class));
    }

    @Test
    @DisplayName("Should throw BusinessException when completing reconciliation with non-zero difference")
    void testCompleteReconciliationFailsWhenUnbalanced() {
        BankReconciliation recon = new BankReconciliation();
        recon.setId(5L);
        recon.setBankAccountId(10L);
        recon.setStatementDate(LocalDate.of(2026, 10, 31));
        recon.setStatementBalance(BigDecimal.valueOf(1500));
        recon.setBookBalance(BigDecimal.valueOf(1000));
        recon.setDifference(BigDecimal.valueOf(500));
        recon.setStatus(ReconciliationStatus.IN_PROGRESS);

        when(reconciliationRepository.findById(5L)).thenReturn(Optional.of(recon));
        when(bankAccountRepository.findByCompanyIdAndId(1L, 10L)).thenReturn(Optional.of(testBankAccount));
        when(accountRepository.findByCompanyIdAndId(1L, 101L)).thenReturn(Optional.of(testGlAccount));
        when(journalLineRepository.findAllPostedLinesForAccount(1L, 101L, recon.getStatementDate())).thenReturn(Collections.emptyList());

        BusinessException ex = assertThrows(BusinessException.class, () -> reconciliationService.completeReconciliation(5L));
        assertEquals("RECONCILIATION_NOT_BALANCED", ex.getCode());
        assertTrue(ex.getMessage().contains("must be 0.00"));
    }

    @Test
    @DisplayName("Should successfully complete reconciliation when difference is strictly 0.00")
    void testCompleteReconciliationSucceedsWhenBalanced() {
        BankReconciliation recon = new BankReconciliation();
        recon.setId(5L);
        recon.setBankAccountId(10L);
        recon.setStatementDate(LocalDate.of(2026, 10, 31));
        recon.setStatementBalance(BigDecimal.valueOf(1000));
        recon.setBookBalance(BigDecimal.valueOf(1000));
        recon.setDifference(BigDecimal.ZERO);
        recon.setStatus(ReconciliationStatus.IN_PROGRESS);

        when(reconciliationRepository.findById(5L)).thenReturn(Optional.of(recon));
        when(bankAccountRepository.findByCompanyIdAndId(1L, 10L)).thenReturn(Optional.of(testBankAccount));
        when(accountRepository.findByCompanyIdAndId(1L, 101L)).thenReturn(Optional.of(testGlAccount));
        when(journalLineRepository.findAllPostedLinesForAccount(1L, 101L, recon.getStatementDate())).thenReturn(Collections.emptyList());
        when(statementRepository.findByBankAccountIdOrderByStatementDateDesc(10L)).thenReturn(Collections.emptyList());
        when(journalLineRepository.findUnreconciledLinesForAccount(1L, 101L, recon.getStatementDate())).thenReturn(Collections.emptyList());

        BankReconciliationDto result = reconciliationService.completeReconciliation(5L);

        assertNotNull(result);
        assertEquals(ReconciliationStatus.COMPLETED, result.status());
        assertNotNull(result.completedAt());
        assertEquals(0, result.difference().compareTo(BigDecimal.ZERO));
    }
}

