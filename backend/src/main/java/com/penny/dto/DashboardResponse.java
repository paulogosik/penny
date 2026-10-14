package com.penny.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record DashboardResponse(
    String month,
    BigDecimal totalIncome,
    BigDecimal totalExpenses,
    BigDecimal balance,
    List<CategorySpending> byCategory,
    List<CreditCardBill> creditCardBills
) {

    public record CategorySpending(
        UUID categoryId,
        String name,
        String color,
        BigDecimal total,
        BigDecimal monthlyBudget,
        BigDecimal budgetUsedPercent
    ) {
    }

    public record CreditCardBill(
        UUID creditCardId,
        String name,
        LocalDate billingMonth,
        LocalDate dueDate,
        BigDecimal total
    ) {
    }
}
