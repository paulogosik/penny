package com.penny.dto;

import com.penny.model.Goal;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record GoalResponse(
    UUID id,
    String name,
    BigDecimal targetAmount,
    BigDecimal savedAmount,
    LocalDate targetDate,
    Instant achievedAt,
    BigDecimal progressPercent
) {

    public static GoalResponse from(Goal goal) {
        BigDecimal progress = goal.getSavedAmount()
            .multiply(BigDecimal.valueOf(100))
            .divide(goal.getTargetAmount(), 1, RoundingMode.HALF_UP);
        return new GoalResponse(
            goal.getId(),
            goal.getName(),
            goal.getTargetAmount(),
            goal.getSavedAmount(),
            goal.getTargetDate(),
            goal.getAchievedAt(),
            progress
        );
    }
}
