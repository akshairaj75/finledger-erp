package com.backend.finledger.banking.service;

import com.backend.finledger.banking.dto.BankAccountDto;

import java.util.List;

public interface BankAccountService {
    List<BankAccountDto> getActiveBankAccounts();
    BankAccountDto getBankAccountById(Long id);
}

