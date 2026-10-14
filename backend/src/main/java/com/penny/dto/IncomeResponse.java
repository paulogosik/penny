package com.penny.dto;

import com.penny.model.Income;
import com.penny.model.IncomeSource;
import com.penny.model.RecurringIncome;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record IncomeResponse(
    UUID id,
    UUID categoryId,
    String categoryName,
    BigDecimal amount,
    String description,
    LocalDate receivedOn,
    IncomeSource source,
    UUID recurringIncomeId,
    Instant createdAt
) {

    public static IncomeResponse from(Income income) {
        RecurringIncome recurring = income.getRecurringIncome();
        return new IncomeResponse(
            income.getId(),
            income.getCategory().getId(),
            income.getCategory().getName(),
            income.getAmount(),
            income.getDescription(),
            income.getReceivedOn(),
            income.getSource(),
            recurring == null ? null : recurring.getId(),
            income.getCreatedAt()
        );
    }
}
