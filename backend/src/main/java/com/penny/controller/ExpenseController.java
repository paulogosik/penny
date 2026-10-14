package com.penny.controller;

import com.penny.dto.ExpenseFilter;
import com.penny.dto.ExpenseRequest;
import com.penny.dto.ExpenseResponse;
import com.penny.dto.PageResponse;
import com.penny.dto.PhotoExpenseRequest;
import com.penny.dto.TextExpenseRequest;
import com.penny.model.User;
import com.penny.service.ExpenseService;
import com.penny.web.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.UUID;

@RestController
@RequestMapping("/api/expenses")
public class ExpenseController {

    private final ExpenseService expenseService;

    public ExpenseController(ExpenseService expenseService) {
        this.expenseService = expenseService;
    }

    @PostMapping("/text")
    @ResponseStatus(HttpStatus.CREATED)
    public ExpenseResponse recordFromText(@CurrentUser User user, @Valid @RequestBody TextExpenseRequest request) {
        return expenseService.recordFromText(user, request.text());
    }

    @PostMapping("/photo")
    @ResponseStatus(HttpStatus.CREATED)
    public ExpenseResponse recordFromPhoto(@CurrentUser User user, @Valid @RequestBody PhotoExpenseRequest request) {
        return expenseService.recordFromReceipt(user, request.imageBase64(), request.mimeType());
    }

    @GetMapping
    public PageResponse<ExpenseResponse> list(
        @CurrentUser User user,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
        @RequestParam(required = false) UUID categoryId,
        @RequestParam(required = false) UUID creditCardId,
        @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth billingMonth,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        ExpenseFilter filter = new ExpenseFilter(from, to, categoryId, creditCardId, billingMonth);
        return expenseService.list(user, filter, page, size);
    }

    @GetMapping("/{id}")
    public ExpenseResponse get(@CurrentUser User user, @PathVariable UUID id) {
        return expenseService.get(user, id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ExpenseResponse create(@CurrentUser User user, @Valid @RequestBody ExpenseRequest request) {
        return expenseService.create(user, request);
    }

    @PutMapping("/{id}")
    public ExpenseResponse update(@CurrentUser User user, @PathVariable UUID id, @Valid @RequestBody ExpenseRequest request) {
        return expenseService.update(user, id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@CurrentUser User user, @PathVariable UUID id) {
        expenseService.delete(user, id);
    }
}
