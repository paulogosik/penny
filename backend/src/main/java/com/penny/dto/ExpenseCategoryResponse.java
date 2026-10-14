package com.penny.dto;

import com.penny.model.ExpenseCategory;

import java.math.BigDecimal;
import java.util.UUID;

public record ExpenseCategoryResponse(
    UUID id,
    String name,
    String color,
    BigDecimal monthlyBudget,
    boolean fallback,
    boolean archived
) {

    public static ExpenseCategoryResponse from(ExpenseCategory category) {
        return new ExpenseCategoryResponse(
            category.getId(),
            category.getName(),
            category.getColor(),
            category.getMonthlyBudget(),
            category.isFallback(),
            category.isArchived()
        );
    }
}
