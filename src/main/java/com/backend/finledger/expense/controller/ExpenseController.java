package com.backend.finledger.expense.controller;

import com.backend.finledger.common.PageResponse;
import com.backend.finledger.expense.dto.*;
import com.backend.finledger.expense.entity.ExpenseStatus;
import com.backend.finledger.expense.service.ExpenseService;
import com.backend.finledger.expense.service.ReimbursementService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/expenses")
public class ExpenseController {

    private final ExpenseService expenseService;
    private final ReimbursementService reimbursementService;

    public ExpenseController(ExpenseService expenseService, ReimbursementService reimbursementService) {
        this.expenseService = expenseService;
        this.reimbursementService = reimbursementService;
    }

    @GetMapping("/billable")
    @PreAuthorize("hasAuthority('INVOICE_CREATE') or hasAuthority('EXPENSE_VIEW')")
    public ResponseEntity<List<ExpenseLineDto>> getBillableExpenses(
            @RequestParam(required = false) Long customerId) {
        return ResponseEntity.ok(expenseService.getBillableExpenseLines(customerId));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('EXPENSE_VIEW')")
    public ResponseEntity<PageResponse<ExpenseDto>> getExpenses(
            @RequestParam(required = false, defaultValue = "all") String tab,
            @RequestParam(required = false) ExpenseStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            Pageable pageable) {
        return ResponseEntity.ok(expenseService.getExpenses(tab, status, from, to, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('EXPENSE_VIEW')")
    public ResponseEntity<ExpenseDto> getExpenseById(@PathVariable Long id) {
        return ResponseEntity.ok(expenseService.getExpenseById(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('EXPENSE_CREATE')")
    public ResponseEntity<ExpenseDto> createExpense(@Valid @RequestBody CreateExpenseRequest request) {
        ExpenseDto dto = expenseService.createExpense(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('EXPENSE_CREATE')")
    public ResponseEntity<ExpenseDto> updateExpense(
            @PathVariable Long id,
            @Valid @RequestBody UpdateExpenseRequest request) {
        return ResponseEntity.ok(expenseService.updateExpense(id, request));
    }

    @PostMapping("/{id}/submit")
    @PreAuthorize("hasAuthority('EXPENSE_CREATE')")
    public ResponseEntity<ExpenseDto> submitExpense(@PathVariable Long id) {
        return ResponseEntity.ok(expenseService.submitExpense(id));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('EXPENSE_APPROVE')")
    public ResponseEntity<ExpenseDto> approveExpense(
            @PathVariable Long id,
            @RequestBody(required = false) ApproveExpenseRequest request) {
        String comment = request != null ? request.comment() : null;
        return ResponseEntity.ok(expenseService.approveExpense(id, comment));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAuthority('EXPENSE_APPROVE')")
    public ResponseEntity<ExpenseDto> rejectExpense(
            @PathVariable Long id,
            @Valid @RequestBody RejectExpenseRequest request) {
        return ResponseEntity.ok(expenseService.rejectExpense(id, request.reason()));
    }

    @PostMapping("/reimburse")
    @PreAuthorize("hasAuthority('EXPENSE_REIMBURSE')")
    public ResponseEntity<BatchReimburseResponse> reimburseExpenses(
            @Valid @RequestBody BatchReimburseRequest request) {
        return ResponseEntity.ok(reimbursementService.reimburseExpenses(request));
    }
}

