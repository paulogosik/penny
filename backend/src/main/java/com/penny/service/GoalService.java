package com.penny.service;

import com.penny.dto.GoalRequest;
import com.penny.dto.GoalResponse;
import com.penny.exception.NotFoundException;
import com.penny.model.Goal;
import com.penny.model.User;
import com.penny.repository.GoalRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.List;
import java.util.UUID;

@Service
public class GoalService {

    private final GoalRepository goals;
    private final Clock clock;

    public GoalService(GoalRepository goals, Clock clock) {
        this.goals = goals;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<GoalResponse> list(User user) {
        return goals.findByUserOrderByCreatedAtAsc(user).stream().map(GoalResponse::from).toList();
    }

    @Transactional
    public GoalResponse create(User user, GoalRequest request) {
        Goal goal = new Goal(user);
        apply(goal, request);
        return GoalResponse.from(goals.save(goal));
    }

    @Transactional
    public GoalResponse update(User user, UUID id, GoalRequest request) {
        Goal goal = find(user, id);
        apply(goal, request);
        return GoalResponse.from(goal);
    }

    @Transactional
    public void delete(User user, UUID id) {
        goals.delete(find(user, id));
    }

    private void apply(Goal goal, GoalRequest request) {
        goal.setName(request.name().strip());
        goal.setTargetAmount(request.targetAmount());
        goal.setSavedAmount(request.savedAmount() == null ? BigDecimal.ZERO : request.savedAmount());
        goal.setTargetDate(request.targetDate());
        goal.refreshAchievement(clock.instant());
    }

    private Goal find(User user, UUID id) {
        return goals.findByIdAndUser(id, user)
            .orElseThrow(() -> new NotFoundException("GOAL_NOT_FOUND", "Meta não encontrada."));
    }
}
