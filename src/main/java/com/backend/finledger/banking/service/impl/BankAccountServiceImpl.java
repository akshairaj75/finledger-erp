package com.backend.finledger.banking.service.impl;

import com.backend.finledger.banking.dto.BankAccountDto;
import com.backend.finledger.banking.entity.BankAccount;
import com.backend.finledger.banking.repository.BankAccountRepository;
import com.backend.finledger.banking.service.BankAccountService;
import com.backend.finledger.common.BusinessException;
import com.backend.finledger.security.CurrentUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class BankAccountServiceImpl implements BankAccountService {

    private final BankAccountRepository bankAccountRepository;

    public BankAccountServiceImpl(BankAccountRepository bankAccountRepository) {
        this.bankAccountRepository = bankAccountRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<BankAccountDto> getActiveBankAccounts() {
        Long companyId = CurrentUser.getCompanyId();
        return bankAccountRepository.findByCompanyIdAndIsActiveTrue(companyId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public BankAccountDto getBankAccountById(Long id) {
        Long companyId = CurrentUser.getCompanyId();
        BankAccount account = bankAccountRepository.findByCompanyIdAndId(companyId, id)
                .orElseThrow(() -> new BusinessException("BANK_ACCOUNT_NOT_FOUND", "Bank account not found with id: " + id));
        return toDto(account);
    }

    private BankAccountDto toDto(BankAccount a) {
        return new BankAccountDto(
                a.getId(),
                a.getCompanyId(),
                a.getName(),
                a.getBankName(),
                a.getAccountNumber(),
                a.getIban(),
                a.getSwiftCode(),
                a.getCurrencyCode(),
                a.getGlAccountId(),
                a.getAccountKind(),
                a.getIsActive()
        );
    }
}

