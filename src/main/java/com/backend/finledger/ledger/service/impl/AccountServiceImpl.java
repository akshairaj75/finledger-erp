package com.backend.finledger.ledger.service.impl;

import com.backend.finledger.common.BusinessException;
import com.backend.finledger.common.PageResponse;
import com.backend.finledger.ledger.dto.AccountDto;
import com.backend.finledger.ledger.dto.AccountNodeDto;
import com.backend.finledger.ledger.dto.AccountRequest;
import com.backend.finledger.ledger.entity.Account;
import com.backend.finledger.ledger.entity.AccountType;
import com.backend.finledger.ledger.entity.JournalSourceType;
import com.backend.finledger.ledger.entity.JournalStatus;
import com.backend.finledger.ledger.mapper.AccountMapper;
import com.backend.finledger.ledger.repository.AccountRepository;
import com.backend.finledger.ledger.repository.JournalEntryRepository;
import com.backend.finledger.ledger.service.AccountService;
import com.backend.finledger.security.CurrentUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

/**
 * Chart of Accounts service.
 *
 * <p><b>Opening Balance Policy:</b> {@code accounts.openingBalance} stores static pre-ledger opening balances
 * that {@code ReportServiceImpl} and {@code BankReconciliationServiceImpl} add directly to posted journal movements.
 * To prevent double-counting or out-of-balance ledgers, direct account opening balances cannot be set on group
 * accounts or while posted {@link JournalSourceType#OPENING} journal entries exist for the company.
 */
@Service
public class AccountServiceImpl implements AccountService {

    private final AccountRepository accountRepository;
    private final AccountMapper accountMapper;
    private final JournalEntryRepository journalEntryRepository;

    public AccountServiceImpl(AccountRepository accountRepository, AccountMapper accountMapper) {
        this(accountRepository, accountMapper, null);
    }

    @Autowired
    public AccountServiceImpl(AccountRepository accountRepository,
                              AccountMapper accountMapper,
                              JournalEntryRepository journalEntryRepository) {
        this.accountRepository = accountRepository;
        this.accountMapper = accountMapper;
        this.journalEntryRepository = journalEntryRepository;
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyAuthority('ACCOUNT_VIEW','JOURNAL_VIEW','JOURNAL_CREATE','INVOICE_CREATE','BILL_CREATE','SETTINGS_VIEW','SETTINGS_EDIT')")
    public PageResponse<AccountDto> getAccounts(AccountType type, Boolean active, String q, Pageable pageable) {
        Long companyId = CurrentUser.getCompanyId();
        return PageResponse.from(accountRepository.searchAccounts(companyId, type, active, q, pageable)
                .map(accountMapper::toDto));
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyAuthority('ACCOUNT_VIEW','JOURNAL_VIEW','JOURNAL_CREATE','INVOICE_CREATE','BILL_CREATE','SETTINGS_VIEW','SETTINGS_EDIT')")
    public List<AccountNodeDto> getAccountsTree() {
        Long companyId = CurrentUser.getCompanyId();
        List<Account> allAccounts = accountRepository.findByCompanyIdOrderByCodeAsc(companyId);

        Map<Long, AccountNodeDto> nodeMap = new LinkedHashMap<>();
        List<AccountNodeDto> roots = new ArrayList<>();

        // First pass: create node DTOs
        for (Account acc : allAccounts) {
            AccountDto dto = accountMapper.toDto(acc);
            nodeMap.put(acc.getId(), new AccountNodeDto(dto));
        }

        // Second pass: build hierarchy
        for (Account acc : allAccounts) {
            AccountNodeDto node = nodeMap.get(acc.getId());
            if (acc.getParentId() != null && nodeMap.containsKey(acc.getParentId())) {
                nodeMap.get(acc.getParentId()).getChildren().add(node);
            } else {
                roots.add(node);
            }
        }

        return roots;
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyAuthority('ACCOUNT_VIEW','JOURNAL_VIEW','JOURNAL_CREATE','INVOICE_CREATE','BILL_CREATE','SETTINGS_VIEW','SETTINGS_EDIT')")
    public AccountDto getAccountById(Long id) {
        Long companyId = CurrentUser.getCompanyId();
        Account account = accountRepository.findByCompanyIdAndId(companyId, id)
                .orElseThrow(() -> new BusinessException("ACCOUNT_NOT_FOUND", "Account not found with id: " + id));
        return accountMapper.toDto(account);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('ACCOUNT_CREATE') or hasAuthority('SETTINGS_EDIT')")
    public AccountDto createAccount(AccountRequest request) {
        Long companyId = CurrentUser.getCompanyId();

        if (accountRepository.findByCompanyIdAndCode(companyId, request.code()).isPresent()) {
            throw new BusinessException("ACCOUNT_CODE_EXISTS", "Account code already exists: " + request.code());
        }

        if (request.parentId() != null) {
            Account parent = accountRepository.findByCompanyIdAndId(companyId, request.parentId())
                    .orElseThrow(() -> new BusinessException("PARENT_ACCOUNT_NOT_FOUND", "Parent account not found: " + request.parentId()));
            if (!Boolean.TRUE.equals(parent.getIsGroup())) {
                parent.setIsGroup(true);
                accountRepository.save(parent);
            }
        }

        BigDecimal openingBalance = request.openingBalance() != null ? request.openingBalance() : BigDecimal.ZERO;
        boolean isGroup = request.isGroup() != null ? request.isGroup() : false;
        validateOpeningBalancePolicy(companyId, isGroup, openingBalance, BigDecimal.ZERO);

        Account account = new Account(
                companyId,
                request.code().trim(),
                request.name().trim(),
                request.accountType(),
                request.subType(),
                request.parentId(),
                isGroup,
                request.normalBalance()
        );
        account.setCurrencyCode(request.currencyCode());
        account.setOpeningBalance(openingBalance);
        account.setIsSystem(false);
        account.setIsActive(request.isActive() != null ? request.isActive() : true);

        Account saved = accountRepository.save(account);
        return accountMapper.toDto(saved);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('ACCOUNT_EDIT') or hasAuthority('SETTINGS_EDIT')")
    public AccountDto updateAccount(Long id, AccountRequest request) {
        Long companyId = CurrentUser.getCompanyId();
        Account account = accountRepository.findByCompanyIdAndId(companyId, id)
                .orElseThrow(() -> new BusinessException("ACCOUNT_NOT_FOUND", "Account not found with id: " + id));

        boolean effectiveIsGroup = request.isGroup() != null ? request.isGroup() : Boolean.TRUE.equals(account.getIsGroup());
        BigDecimal existingOpening = account.getOpeningBalance() != null ? account.getOpeningBalance() : BigDecimal.ZERO;
        BigDecimal requestedOpening = request.openingBalance() != null ? request.openingBalance() : existingOpening;
        validateOpeningBalancePolicy(companyId, effectiveIsGroup, requestedOpening, existingOpening);

        account.setName(request.name().trim());
        account.setSubType(request.subType());
        account.setParentId(request.parentId());
        if (request.isGroup() != null) {
            account.setIsGroup(request.isGroup());
        }
        account.setNormalBalance(request.normalBalance());
        account.setCurrencyCode(request.currencyCode());
        if (request.openingBalance() != null) {
            account.setOpeningBalance(request.openingBalance());
        }
        if (request.isActive() != null) {
            account.setIsActive(request.isActive());
        }

        Account saved = accountRepository.save(account);
        return accountMapper.toDto(saved);
    }

    private void validateOpeningBalancePolicy(Long companyId, boolean isGroup, BigDecimal requestedOpening, BigDecimal existingOpening) {
        if (requestedOpening == null || requestedOpening.compareTo(BigDecimal.ZERO) == 0) {
            return;
        }
        if (isGroup) {
            throw new BusinessException("GROUP_ACCOUNT_OPENING_BALANCE",
                    "Opening balance cannot be assigned to a group account");
        }
        boolean openingChanged = existingOpening == null || requestedOpening.compareTo(existingOpening) != 0;
        if (openingChanged && journalEntryRepository != null
                && journalEntryRepository.existsByCompanyIdAndSourceTypeAndStatus(companyId, JournalSourceType.OPENING, JournalStatus.POSTED)) {
            throw new BusinessException("OPENING_JOURNAL_EXISTS",
                    "Cannot modify account openingBalance while posted OPENING journal entries exist; post an OPENING journal entry instead to keep ledgers balanced.");
        }
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('ACCOUNT_DELETE') or hasAuthority('SETTINGS_EDIT')")
    public void deleteAccount(Long id) {
        Long companyId = CurrentUser.getCompanyId();
        Account account = accountRepository.findByCompanyIdAndId(companyId, id)
                .orElseThrow(() -> new BusinessException("ACCOUNT_NOT_FOUND", "Account not found with id: " + id));

        if (Boolean.TRUE.equals(account.getIsSystem())) {
            throw new BusinessException("SYSTEM_ACCOUNT_IMMUTABLE", "System accounts cannot be deleted");
        }

        long usageCount = accountRepository.countJournalLinesByAccountId(id);
        if (usageCount > 0) {
            throw new BusinessException("ACCOUNT_IN_USE", "Cannot delete account with existing transactions (" + usageCount + " journal lines)");
        }

        accountRepository.delete(account);
    }
}

