package com.penny.dto;

import com.penny.model.RecurringIncome;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record RecurringIncomeResponse(
    UUID id,
    UUID categoryId,
    String categoryName,
    String description,
    BigDecimal amount,
    Integer dayOfMonth,
    LocalDate startsOn,
    LocalDate endsOn,
    boolean active,
    LocalDate lastGeneratedMonth
) {

    public static RecurringIncomeResponse from(RecurringIncome recurring) {
        return new RecurringIncomeResponse(
            recurring.getId(),
            recurring.getCategory().getId(),
            recurring.getCategory().getName(),
            recurring.getDescription(),
            recurring.getAmount(),
            recurring.getDayOfMonth(),
            recurring.getStartsOn(),
            recurring.getEndsOn(),
            recurring.isActive(),
            recurring.getLastGeneratedMonth()
        );
    }
}
