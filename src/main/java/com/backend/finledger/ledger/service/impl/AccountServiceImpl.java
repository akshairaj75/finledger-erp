package com.backend.finledger.ledger.service.impl;

import com.backend.finledger.common.BusinessException;
import com.backend.finledger.common.PageResponse;
import com.backend.finledger.ledger.dto.AccountDto;
import com.backend.finledger.ledger.dto.AccountNodeDto;
import com.backend.finledger.ledger.dto.AccountRequest;
import com.backend.finledger.ledger.entity.Account;
import com.backend.finledger.ledger.entity.AccountType;
import com.backend.finledger.ledger.mapper.AccountMapper;
import com.backend.finledger.ledger.repository.AccountRepository;
import com.backend.finledger.ledger.service.AccountService;
import com.backend.finledger.security.CurrentUser;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

@Service
public class AccountServiceImpl implements AccountService {

    private final AccountRepository accountRepository;
    private final AccountMapper accountMapper;

    public AccountServiceImpl(AccountRepository accountRepository, AccountMapper accountMapper) {
        this.accountRepository = accountRepository;
        this.accountMapper = accountMapper;
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('ACCOUNT_VIEW') or isAuthenticated()")
    public PageResponse<AccountDto> getAccounts(AccountType type, Boolean active, String q, Pageable pageable) {
        Long companyId = CurrentUser.getCompanyId();
        return PageResponse.from(accountRepository.searchAccounts(companyId, type, active, q, pageable)
                .map(accountMapper::toDto));
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('ACCOUNT_VIEW') or isAuthenticated()")
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
    @PreAuthorize("hasAuthority('ACCOUNT_VIEW') or isAuthenticated()")
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

        Account account = new Account(
                companyId,
                request.code().trim(),
                request.name().trim(),
                request.accountType(),
                request.subType(),
                request.parentId(),
                request.isGroup() != null ? request.isGroup() : false,
                request.normalBalance()
        );
        account.setCurrencyCode(request.currencyCode());
        account.setOpeningBalance(request.openingBalance() != null ? request.openingBalance() : BigDecimal.ZERO);
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

