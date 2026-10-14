package com.penny.dto;

import com.penny.model.CreditCard;
import com.penny.model.Expense;
import com.penny.model.ExpenseSource;
import com.penny.model.InstallmentPlan;
import com.penny.model.RecurringExpense;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ExpenseResponse(
    UUID id,
    UUID categoryId,
    String categoryName,
    BigDecimal amount,
    String description,
    LocalDate occurredOn,
    ExpenseSource source,
    UUID creditCardId,
    String creditCardName,
    LocalDate billingMonth,
    InstallmentInfo installment,
    UUID recurringExpenseId,
    Instant createdAt
) {

    public record InstallmentInfo(UUID planId, Integer number, Integer count, BigDecimal totalAmount) {
    }

    public static ExpenseResponse from(Expense expense) {
        CreditCard card = expense.getCreditCard();
        InstallmentPlan plan = expense.getInstallmentPlan();
        RecurringExpense recurring = expense.getRecurringExpense();
        return new ExpenseResponse(
            expense.getId(),
            expense.getCategory().getId(),
            expense.getCategory().getName(),
            expense.getAmount(),
            expense.getDescription(),
            expense.getOccurredOn(),
            expense.getSource(),
            card == null ? null : card.getId(),
            card == null ? null : card.getName(),
            expense.getBillingMonth(),
            plan == null ? null : new InstallmentInfo(
                plan.getId(),
                expense.getInstallmentNumber(),
                plan.getInstallmentsCount(),
                plan.getTotalAmount()
            ),
            recurring == null ? null : recurring.getId(),
            expense.getCreatedAt()
        );
    }
}
