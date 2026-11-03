package com.backend.finledger.ledger.service;

import com.backend.finledger.common.PageResponse;
import com.backend.finledger.ledger.dto.AccountDto;
import com.backend.finledger.ledger.dto.AccountNodeDto;
import com.backend.finledger.ledger.dto.AccountRequest;
import com.backend.finledger.ledger.entity.AccountType;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface AccountService {
    PageResponse<AccountDto> getAccounts(AccountType type, Boolean active, String q, Pageable pageable);
    List<AccountNodeDto> getAccountsTree();
    AccountDto getAccountById(Long id);
    AccountDto createAccount(AccountRequest request);
    AccountDto updateAccount(Long id, AccountRequest request);
    void deleteAccount(Long id);
}

