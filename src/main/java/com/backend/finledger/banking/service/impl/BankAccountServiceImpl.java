package com.backend.finledger.banking.service.impl;

import com.backend.finledger.banking.dto.BankAccountDto;
import com.backend.finledger.banking.dto.BankAccountRequest;
import com.backend.finledger.banking.entity.AccountKind;
import com.backend.finledger.banking.entity.BankAccount;
import com.backend.finledger.banking.repository.BankAccountRepository;
import com.backend.finledger.banking.service.BankAccountService;
import com.backend.finledger.common.BusinessException;
import com.backend.finledger.ledger.repository.AccountRepository;
import com.backend.finledger.security.CurrentUser;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class BankAccountServiceImpl implements BankAccountService {

    private final BankAccountRepository bankAccountRepository;
    private final AccountRepository accountRepository;

    public BankAccountServiceImpl(BankAccountRepository bankAccountRepository, AccountRepository accountRepository) {
        this.bankAccountRepository = bankAccountRepository;
        this.accountRepository = accountRepository;
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyAuthority('BANK_RECONCILE','COMPANY_VIEW','SETTINGS_VIEW','SETTINGS_EDIT','RECEIPT_CREATE','RECEIPT_VIEW','PAYMENT_CREATE','PAYMENT_VIEW','EXPENSE_CREATE','EXPENSE_VIEW','EXPENSE_REIMBURSE')")
    public List<BankAccountDto> getActiveBankAccounts() {
        Long companyId = CurrentUser.getCompanyId();
        return bankAccountRepository.findByCompanyIdAndIsActiveTrue(companyId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyAuthority('BANK_RECONCILE','COMPANY_VIEW','SETTINGS_VIEW','SETTINGS_EDIT','RECEIPT_CREATE','RECEIPT_VIEW','PAYMENT_CREATE','PAYMENT_VIEW','EXPENSE_CREATE','EXPENSE_VIEW','EXPENSE_REIMBURSE')")
    public List<BankAccountDto> getAllBankAccounts() {
        Long companyId = CurrentUser.getCompanyId();
        return bankAccountRepository.findByCompanyId(companyId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyAuthority('BANK_RECONCILE','COMPANY_VIEW','SETTINGS_VIEW','SETTINGS_EDIT','RECEIPT_CREATE','RECEIPT_VIEW','PAYMENT_CREATE','PAYMENT_VIEW','EXPENSE_CREATE','EXPENSE_VIEW','EXPENSE_REIMBURSE')")
    public BankAccountDto getBankAccountById(Long id) {
        Long companyId = CurrentUser.getCompanyId();
        BankAccount account = bankAccountRepository.findByCompanyIdAndId(companyId, id)
                .orElseThrow(() -> new BusinessException("BANK_ACCOUNT_NOT_FOUND", "Bank account not found with id: " + id));
        return toDto(account);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('BANK_RECONCILE') or hasAuthority('SETTINGS_EDIT')")
    public BankAccountDto createBankAccount(BankAccountRequest request) {
        Long companyId = CurrentUser.getCompanyId();

        // Validate GL Account
        accountRepository.findByCompanyIdAndId(companyId, request.glAccountId())
                .orElseThrow(() -> new BusinessException("GL_ACCOUNT_NOT_FOUND", "GL account not found with id: " + request.glAccountId()));

        // Check if GL account already linked
        Optional<BankAccount> existing = bankAccountRepository.findByCompanyIdAndGlAccountId(companyId, request.glAccountId());
        if (existing.isPresent()) {
            throw new BusinessException("GL_ACCOUNT_ALREADY_LINKED", "GL Account is already linked to bank account: " + existing.get().getName());
        }

        BankAccount account = new BankAccount();
        account.setCompanyId(companyId);
        account.setName(request.name());
        account.setBankName(request.bankName());
        account.setAccountNumber(request.accountNumber());
        account.setIban(request.iban());
        account.setSwiftCode(request.swiftCode());
        account.setCurrencyCode(request.currencyCode());
        account.setGlAccountId(request.glAccountId());
        account.setAccountKind(request.accountKind() != null ? request.accountKind() : AccountKind.BANK);
        account.setIsActive(request.isActive() != null ? request.isActive() : true);

        BankAccount saved = bankAccountRepository.save(account);
        return toDto(saved);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('BANK_RECONCILE') or hasAuthority('SETTINGS_EDIT')")
    public BankAccountDto updateBankAccount(Long id, BankAccountRequest request) {
        Long companyId = CurrentUser.getCompanyId();
        BankAccount account = bankAccountRepository.findByCompanyIdAndId(companyId, id)
                .orElseThrow(() -> new BusinessException("BANK_ACCOUNT_NOT_FOUND", "Bank account not found with id: " + id));

        // If changing GL account, validate
        if (!account.getGlAccountId().equals(request.glAccountId())) {
            accountRepository.findByCompanyIdAndId(companyId, request.glAccountId())
                    .orElseThrow(() -> new BusinessException("GL_ACCOUNT_NOT_FOUND", "GL account not found with id: " + request.glAccountId()));

            Optional<BankAccount> existing = bankAccountRepository.findByCompanyIdAndGlAccountId(companyId, request.glAccountId());
            if (existing.isPresent() && !existing.get().getId().equals(id)) {
                throw new BusinessException("GL_ACCOUNT_ALREADY_LINKED", "GL Account is already linked to bank account: " + existing.get().getName());
            }
            account.setGlAccountId(request.glAccountId());
        }

        account.setName(request.name());
        account.setBankName(request.bankName());
        account.setAccountNumber(request.accountNumber());
        account.setIban(request.iban());
        account.setSwiftCode(request.swiftCode());
        account.setCurrencyCode(request.currencyCode());
        if (request.accountKind() != null) account.setAccountKind(request.accountKind());
        if (request.isActive() != null) account.setIsActive(request.isActive());

        BankAccount saved = bankAccountRepository.save(account);
        return toDto(saved);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('BANK_RECONCILE') or hasAuthority('SETTINGS_EDIT')")
    public void deleteBankAccount(Long id) {
        Long companyId = CurrentUser.getCompanyId();
        BankAccount account = bankAccountRepository.findByCompanyIdAndId(companyId, id)
                .orElseThrow(() -> new BusinessException("BANK_ACCOUNT_NOT_FOUND", "Bank account not found with id: " + id));
        account.setIsActive(false);
        bankAccountRepository.save(account);
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
