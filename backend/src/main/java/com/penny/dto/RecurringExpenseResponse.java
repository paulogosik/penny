package com.penny.dto;

import com.penny.model.CreditCard;
import com.penny.model.RecurringExpense;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record RecurringExpenseResponse(
    UUID id,
    UUID categoryId,
    String categoryName,
    UUID creditCardId,
    String creditCardName,
    String description,
    BigDecimal amount,
    Integer dayOfMonth,
    LocalDate startsOn,
    LocalDate endsOn,
    boolean active,
    LocalDate lastGeneratedMonth
) {

    public static RecurringExpenseResponse from(RecurringExpense recurring) {
        CreditCard card = recurring.getCreditCard();
        return new RecurringExpenseResponse(
            recurring.getId(),
            recurring.getCategory().getId(),
            recurring.getCategory().getName(),
            card == null ? null : card.getId(),
            card == null ? null : card.getName(),
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
