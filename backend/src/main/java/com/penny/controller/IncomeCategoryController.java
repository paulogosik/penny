package com.penny.controller;

import com.penny.dto.IncomeCategoryRequest;
import com.penny.dto.IncomeCategoryResponse;
import com.penny.model.User;
import com.penny.service.IncomeCategoryService;
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
@RequestMapping("/api/income-categories")
public class IncomeCategoryController {

    private final IncomeCategoryService categoryService;

    public IncomeCategoryController(IncomeCategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    public List<IncomeCategoryResponse> list(
        @CurrentUser User user,
        @RequestParam(defaultValue = "false") boolean includeArchived
    ) {
        return categoryService.list(user, includeArchived);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public IncomeCategoryResponse create(@CurrentUser User user, @Valid @RequestBody IncomeCategoryRequest request) {
        return categoryService.create(user, request);
    }

    @PutMapping("/{id}")
    public IncomeCategoryResponse update(
        @CurrentUser User user,
        @PathVariable UUID id,
        @Valid @RequestBody IncomeCategoryRequest request
    ) {
        return categoryService.update(user, id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@CurrentUser User user, @PathVariable UUID id) {
        categoryService.delete(user, id);
    }
}
