package com.backend.finledger.expense.service.impl;

import com.backend.finledger.common.BusinessException;
import com.backend.finledger.common.PageResponse;
import com.backend.finledger.expense.dto.ExpenseCategoryDto;
import com.backend.finledger.expense.dto.ExpenseCategoryRequest;
import com.backend.finledger.expense.entity.ExpenseCategory;
import com.backend.finledger.expense.repository.ExpenseCategoryRepository;
import com.backend.finledger.expense.service.ExpenseCategoryService;
import com.backend.finledger.ledger.entity.Account;
import com.backend.finledger.ledger.repository.AccountRepository;
import com.backend.finledger.security.CurrentUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ExpenseCategoryServiceImpl implements ExpenseCategoryService {

    private final ExpenseCategoryRepository categoryRepository;
    private final AccountRepository accountRepository;

    public ExpenseCategoryServiceImpl(ExpenseCategoryRepository categoryRepository,
                                      AccountRepository accountRepository) {
        this.categoryRepository = categoryRepository;
        this.accountRepository = accountRepository;
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyAuthority('EXPENSE_VIEW','EXPENSE_CREATE','EXPENSE_APPROVE','SETTINGS_VIEW','SETTINGS_EDIT')")
    public PageResponse<ExpenseCategoryDto> getCategories(Boolean active, String q, Pageable pageable) {
        Long companyId = CurrentUser.getCompanyId();
        Page<ExpenseCategory> page = categoryRepository.searchCategories(companyId, active, q, pageable);
        List<ExpenseCategoryDto> dtos = page.getContent().stream().map(this::toDto).toList();
        return new PageResponse<>(dtos, page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyAuthority('EXPENSE_VIEW','EXPENSE_CREATE','EXPENSE_APPROVE','SETTINGS_VIEW','SETTINGS_EDIT')")
    public List<ExpenseCategoryDto> getAllActiveCategories() {
        Long companyId = CurrentUser.getCompanyId();
        return categoryRepository.findByCompanyIdOrderByNameAsc(companyId).stream()
                .filter(c -> Boolean.TRUE.equals(c.getIsActive()))
                .map(this::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyAuthority('EXPENSE_VIEW','EXPENSE_CREATE','EXPENSE_APPROVE','SETTINGS_VIEW','SETTINGS_EDIT')")
    public ExpenseCategoryDto getCategoryById(Long id) {
        Long companyId = CurrentUser.getCompanyId();
        ExpenseCategory category = categoryRepository.findByCompanyIdAndId(companyId, id)
                .orElseThrow(() -> new BusinessException("CATEGORY_NOT_FOUND", "Expense category not found with id: " + id));
        return toDto(category);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('SETTINGS_EDIT')")
    public ExpenseCategoryDto createCategory(ExpenseCategoryRequest request) {
        Long companyId = CurrentUser.getCompanyId();

        if (categoryRepository.existsByCompanyIdAndName(companyId, request.name().trim())) {
            throw new BusinessException("DUPLICATE_CATEGORY",
                    "Expense category with name '" + request.name() + "' already exists.");
        }

        Account account = accountRepository.findByCompanyIdAndId(companyId, request.accountId())
                .orElseThrow(() -> new BusinessException("ACCOUNT_NOT_FOUND",
                        "Account not found with id: " + request.accountId()));

        if (Boolean.TRUE.equals(account.getIsGroup())) {
            throw new BusinessException("INVALID_ACCOUNT", "Cannot map expense category to a group account.");
        }

        ExpenseCategory category = new ExpenseCategory(
                companyId,
                request.name().trim(),
                request.accountId(),
                request.defaultTaxRateId(),
                request.limitPerClaim(),
                request.receiptRequired() != null ? request.receiptRequired() : true,
                request.isActive() != null ? request.isActive() : true
        );

        ExpenseCategory saved = categoryRepository.save(category);
        return toDto(saved);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('SETTINGS_EDIT')")
    public ExpenseCategoryDto updateCategory(Long id, ExpenseCategoryRequest request) {
        Long companyId = CurrentUser.getCompanyId();

        ExpenseCategory category = categoryRepository.findByCompanyIdAndId(companyId, id)
                .orElseThrow(() -> new BusinessException("CATEGORY_NOT_FOUND", "Expense category not found with id: " + id));

        if (!category.getName().equalsIgnoreCase(request.name().trim())
                && categoryRepository.existsByCompanyIdAndName(companyId, request.name().trim())) {
            throw new BusinessException("DUPLICATE_CATEGORY",
                    "Expense category with name '" + request.name() + "' already exists.");
        }

        Account account = accountRepository.findByCompanyIdAndId(companyId, request.accountId())
                .orElseThrow(() -> new BusinessException("ACCOUNT_NOT_FOUND",
                        "Account not found with id: " + request.accountId()));

        if (Boolean.TRUE.equals(account.getIsGroup())) {
            throw new BusinessException("INVALID_ACCOUNT", "Cannot map expense category to a group account.");
        }

        category.setName(request.name().trim());
        category.setAccountId(request.accountId());
        category.setDefaultTaxRateId(request.defaultTaxRateId());
        category.setLimitPerClaim(request.limitPerClaim());
        if (request.receiptRequired() != null) {
            category.setReceiptRequired(request.receiptRequired());
        }
        if (request.isActive() != null) {
            category.setIsActive(request.isActive());
        }

        ExpenseCategory saved = categoryRepository.save(category);
        return toDto(saved);
    }

    private ExpenseCategoryDto toDto(ExpenseCategory cat) {
        Account account = accountRepository.findByCompanyIdAndId(cat.getCompanyId(), cat.getAccountId()).orElse(null);
        String accountCode = account != null ? account.getCode() : null;
        String accountName = account != null ? account.getName() : null;

        return new ExpenseCategoryDto(
                cat.getId(),
                cat.getCompanyId(),
                cat.getName(),
                cat.getAccountId(),
                accountCode,
                accountName,
                cat.getDefaultTaxRateId(),
                cat.getLimitPerClaim(),
                cat.getReceiptRequired(),
                cat.getIsActive()
        );
    }
}

