package com.backend.finledger.expense.controller;

import com.backend.finledger.common.PageResponse;
import com.backend.finledger.expense.dto.ExpenseCategoryDto;
import com.backend.finledger.expense.dto.ExpenseCategoryRequest;
import com.backend.finledger.expense.service.ExpenseCategoryService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/expense-categories")
public class ExpenseCategoryController {

    private final ExpenseCategoryService categoryService;

    public ExpenseCategoryController(ExpenseCategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('EXPENSE_VIEW','EXPENSE_CREATE','EXPENSE_APPROVE','SETTINGS_VIEW','SETTINGS_EDIT')")
    public ResponseEntity<PageResponse<ExpenseCategoryDto>> getCategories(
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) String q,
            Pageable pageable) {
        return ResponseEntity.ok(categoryService.getCategories(active, q, pageable));
    }

    @GetMapping("/all")
    @PreAuthorize("hasAnyAuthority('EXPENSE_VIEW','EXPENSE_CREATE','EXPENSE_APPROVE','SETTINGS_VIEW','SETTINGS_EDIT')")
    public ResponseEntity<List<ExpenseCategoryDto>> getAllActiveCategories() {
        return ResponseEntity.ok(categoryService.getAllActiveCategories());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('EXPENSE_VIEW','EXPENSE_CREATE','EXPENSE_APPROVE','SETTINGS_VIEW','SETTINGS_EDIT')")
    public ResponseEntity<ExpenseCategoryDto> getCategoryById(@PathVariable Long id) {
        return ResponseEntity.ok(categoryService.getCategoryById(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('SETTINGS_EDIT')")
    public ResponseEntity<ExpenseCategoryDto> createCategory(@Valid @RequestBody ExpenseCategoryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(categoryService.createCategory(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('SETTINGS_EDIT')")
    public ResponseEntity<ExpenseCategoryDto> updateCategory(
            @PathVariable Long id,
            @Valid @RequestBody ExpenseCategoryRequest request) {
        return ResponseEntity.ok(categoryService.updateCategory(id, request));
    }
}

