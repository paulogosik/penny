package com.penny.controller;

import com.penny.dto.ExpenseRequest;
import com.penny.dto.ExpenseResponse;
import com.penny.service.ExpenseService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/expenses")
public class ExpenseController {

    private final ExpenseService expenseService;

    public ExpenseController(ExpenseService expenseService) {
        this.expenseService = expenseService;
    }

    @PostMapping("/text")
    public ExpenseResponse classifyText(@RequestBody ExpenseRequest request) {
        return expenseService.classifyText(request.text());
    }
}
