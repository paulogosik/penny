package com.penny.controller;

import com.penny.dto.ExpenseCategoryRequest;
import com.penny.dto.ExpenseCategoryResponse;
import com.penny.model.User;
import com.penny.service.ExpenseCategoryService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/expense-categories")
public class ExpenseCategoryController {

    private final ExpenseCategoryService categoryService;

    public ExpenseCategoryController(ExpenseCategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    public List<ExpenseCategoryResponse> list(
        @CurrentUser User user,
        @RequestParam(defaultValue = "false") boolean includeArchived
    ) {
        return categoryService.list(user, includeArchived);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ExpenseCategoryResponse create(@CurrentUser User user, @Valid @RequestBody ExpenseCategoryRequest request) {
        return categoryService.create(user, request);
    }

    @PutMapping("/{id}")
    public ExpenseCategoryResponse update(
        @CurrentUser User user,
        @PathVariable UUID id,
        @Valid @RequestBody ExpenseCategoryRequest request
    ) {
        return categoryService.update(user, id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@CurrentUser User user, @PathVariable UUID id) {
        categoryService.delete(user, id);
    }
}
