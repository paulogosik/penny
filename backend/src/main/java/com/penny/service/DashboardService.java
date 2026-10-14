package com.penny.service;

import com.penny.dto.DashboardResponse;
import com.penny.dto.DashboardResponse.CategorySpending;
import com.penny.dto.DashboardResponse.CreditCardBill;
import com.penny.model.User;
import com.penny.repository.AmountByKey;
import com.penny.repository.CreditCardRepository;
import com.penny.repository.ExpenseCategoryRepository;
import com.penny.repository.ExpenseRepository;
import com.penny.repository.IncomeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    private final ExpenseRepository expenses;
    private final IncomeRepository incomes;
    private final ExpenseCategoryRepository categories;
    private final CreditCardRepository creditCards;
    private final Clock clock;

    public DashboardService(
        ExpenseRepository expenses,
        IncomeRepository incomes,
        ExpenseCategoryRepository categories,
        CreditCardRepository creditCards,
        Clock clock
    ) {
        this.expenses = expenses;
        this.incomes = incomes;
        this.categories = categories;
        this.creditCards = creditCards;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public DashboardResponse summary(User user, YearMonth requestedMonth) {
        YearMonth month = requestedMonth == null ? YearMonth.now(clock.withZone(user.zoneId())) : requestedMonth;
        LocalDate start = month.atDay(1);
        LocalDate end = month.atEndOfMonth();

        BigDecimal totalExpenses = expenses.sumByUserAndPeriod(user, start, end);
        BigDecimal totalIncome = incomes.sumByUserAndPeriod(user, start, end);

        return new DashboardResponse(
            month.toString(),
            totalIncome,
            totalExpenses,
            totalIncome.subtract(totalExpenses),
            spendingByCategory(user, start, end),
            creditCardBills(user, start)
        );
    }

    private List<CategorySpending> spendingByCategory(User user, LocalDate start, LocalDate end) {
        Map<UUID, BigDecimal> totals = toMap(expenses.sumByCategory(user, start, end));
        return categories.findByUserOrderByName(user).stream()
            .filter(category -> totals.containsKey(category.getId())
                || (!category.isArchived() && category.getMonthlyBudget() != null))
            .map(category -> {
                BigDecimal total = totals.getOrDefault(category.getId(), BigDecimal.ZERO);
                return new CategorySpending(
                    category.getId(),
                    category.getName(),
                    category.getColor(),
                    total,
                    category.getMonthlyBudget(),
                    percentOf(total, category.getMonthlyBudget())
                );
            })
            .sorted(Comparator.comparing(CategorySpending::total).reversed())
            .toList();
    }

    private List<CreditCardBill> creditCardBills(User user, LocalDate billingMonth) {
        Map<UUID, BigDecimal> totals = toMap(expenses.sumByCreditCard(user, billingMonth));
        return creditCards.findByUserOrderByName(user).stream()
            .filter(card -> !card.isArchived() || totals.containsKey(card.getId()))
            .map(card -> new CreditCardBill(
                card.getId(),
                card.getName(),
                billingMonth,
                CreditCardBilling.dueDate(card.getDueDay(), billingMonth),
                totals.getOrDefault(card.getId(), BigDecimal.ZERO)
            ))
            .toList();
    }

    private static Map<UUID, BigDecimal> toMap(List<AmountByKey> amounts) {
        return amounts.stream().collect(Collectors.toMap(AmountByKey::key, AmountByKey::total));
    }

    private static BigDecimal percentOf(BigDecimal value, BigDecimal reference) {
        if (reference == null || reference.signum() == 0) {
            return null;
        }
        return value.multiply(BigDecimal.valueOf(100)).divide(reference, 1, RoundingMode.HALF_UP);
    }
}
