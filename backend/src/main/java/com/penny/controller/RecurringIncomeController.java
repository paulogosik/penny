package com.penny.controller;

import com.penny.dto.RecurringIncomeRequest;
import com.penny.dto.RecurringIncomeResponse;
import com.penny.model.User;
import com.penny.service.RecurringIncomeService;
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
@RequestMapping("/api/recurring-incomes")
public class RecurringIncomeController {

    private final RecurringIncomeService recurringIncomeService;

    public RecurringIncomeController(RecurringIncomeService recurringIncomeService) {
        this.recurringIncomeService = recurringIncomeService;
    }

    @GetMapping
    public List<RecurringIncomeResponse> list(@CurrentUser User user) {
        return recurringIncomeService.list(user);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RecurringIncomeResponse create(@CurrentUser User user, @Valid @RequestBody RecurringIncomeRequest request) {
        return recurringIncomeService.create(user, request);
    }

    @PutMapping("/{id}")
    public RecurringIncomeResponse update(
        @CurrentUser User user,
        @PathVariable UUID id,
        @Valid @RequestBody RecurringIncomeRequest request
    ) {
        return recurringIncomeService.update(user, id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@CurrentUser User user, @PathVariable UUID id) {
        recurringIncomeService.delete(user, id);
    }
}
