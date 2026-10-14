package com.penny.controller;

import com.penny.dto.RecurringExpenseRequest;
import com.penny.dto.RecurringExpenseResponse;
import com.penny.model.User;
import com.penny.service.RecurringExpenseService;
import com.penny.web.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/recurring-expenses")
public class RecurringExpenseController {

    private final RecurringExpenseService recurringExpenseService;

    public RecurringExpenseController(RecurringExpenseService recurringExpenseService) {
        this.recurringExpenseService = recurringExpenseService;
    }

    @GetMapping
    public List<RecurringExpenseResponse> list(@CurrentUser User user) {
        return recurringExpenseService.list(user);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RecurringExpenseResponse create(@CurrentUser User user, @Valid @RequestBody RecurringExpenseRequest request) {
        return recurringExpenseService.create(user, request);
    }

    @PutMapping("/{id}")
    public RecurringExpenseResponse update(
        @CurrentUser User user,
        @PathVariable UUID id,
        @Valid @RequestBody RecurringExpenseRequest request
    ) {
        return recurringExpenseService.update(user, id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@CurrentUser User user, @PathVariable UUID id) {
        recurringExpenseService.delete(user, id);
    }
}
