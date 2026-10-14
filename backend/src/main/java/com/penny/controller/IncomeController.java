package com.penny.controller;

import com.penny.dto.IncomeRequest;
import com.penny.dto.IncomeResponse;
import com.penny.model.User;
import com.penny.service.IncomeService;
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
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/incomes")
public class IncomeController {

    private final IncomeService incomeService;

    public IncomeController(IncomeService incomeService) {
        this.incomeService = incomeService;
    }

    @GetMapping
    public List<IncomeResponse> list(
        @CurrentUser User user,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return incomeService.list(user, from, to);
    }

    @GetMapping("/{id}")
    public IncomeResponse get(@CurrentUser User user, @PathVariable UUID id) {
        return incomeService.get(user, id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public IncomeResponse create(@CurrentUser User user, @Valid @RequestBody IncomeRequest request) {
        return incomeService.create(user, request);
    }

    @PutMapping("/{id}")
    public IncomeResponse update(@CurrentUser User user, @PathVariable UUID id, @Valid @RequestBody IncomeRequest request) {
        return incomeService.update(user, id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@CurrentUser User user, @PathVariable UUID id) {
        incomeService.delete(user, id);
    }
}
