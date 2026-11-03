package com.backend.finledger.expense.service;

import com.backend.finledger.common.PageResponse;
import com.backend.finledger.expense.dto.ExpenseCategoryDto;
import com.backend.finledger.expense.dto.ExpenseCategoryRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ExpenseCategoryService {

    PageResponse<ExpenseCategoryDto> getCategories(Boolean active, String q, Pageable pageable);

    List<ExpenseCategoryDto> getAllActiveCategories();

    ExpenseCategoryDto getCategoryById(Long id);

    ExpenseCategoryDto createCategory(ExpenseCategoryRequest request);

    ExpenseCategoryDto updateCategory(Long id, ExpenseCategoryRequest request);
}

