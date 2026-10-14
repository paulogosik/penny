package com.penny.controller;

import com.penny.dto.GoalRequest;
import com.penny.dto.GoalResponse;
import com.penny.model.User;
import com.penny.service.GoalService;
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
@RequestMapping("/api/goals")
public class GoalController {

    private final GoalService goalService;

    public GoalController(GoalService goalService) {
        this.goalService = goalService;
    }

    @GetMapping
    public List<GoalResponse> list(@CurrentUser User user) {
        return goalService.list(user);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public GoalResponse create(@CurrentUser User user, @Valid @RequestBody GoalRequest request) {
        return goalService.create(user, request);
    }

    @PutMapping("/{id}")
    public GoalResponse update(@CurrentUser User user, @PathVariable UUID id, @Valid @RequestBody GoalRequest request) {
        return goalService.update(user, id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@CurrentUser User user, @PathVariable UUID id) {
        goalService.delete(user, id);
    }
}
