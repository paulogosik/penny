package com.penny.controller;

import com.penny.dto.InstallmentPlanRequest;
import com.penny.dto.InstallmentPlanResponse;
import com.penny.model.User;
import com.penny.service.InstallmentPlanService;
import com.penny.web.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/installment-plans")
public class InstallmentPlanController {

    private final InstallmentPlanService installmentPlanService;

    public InstallmentPlanController(InstallmentPlanService installmentPlanService) {
        this.installmentPlanService = installmentPlanService;
    }

    @GetMapping
    public List<InstallmentPlanResponse> list(@CurrentUser User user) {
        return installmentPlanService.list(user);
    }

    @GetMapping("/{id}")
    public InstallmentPlanResponse get(@CurrentUser User user, @PathVariable UUID id) {
        return installmentPlanService.get(user, id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public InstallmentPlanResponse create(@CurrentUser User user, @Valid @RequestBody InstallmentPlanRequest request) {
        return installmentPlanService.create(user, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@CurrentUser User user, @PathVariable UUID id) {
        installmentPlanService.delete(user, id);
    }
}
