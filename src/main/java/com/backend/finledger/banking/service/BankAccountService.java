package com.backend.finledger.banking.service;

import com.backend.finledger.banking.dto.BankAccountDto;
import com.backend.finledger.banking.dto.BankAccountRequest;

import java.util.List;

public interface BankAccountService {
    List<BankAccountDto> getActiveBankAccounts();
    List<BankAccountDto> getAllBankAccounts();
    BankAccountDto getBankAccountById(Long id);
    BankAccountDto createBankAccount(BankAccountRequest request);
    BankAccountDto updateBankAccount(Long id, BankAccountRequest request);
    void deleteBankAccount(Long id);
}
