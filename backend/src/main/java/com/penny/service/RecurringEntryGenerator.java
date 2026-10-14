package com.penny.service;

import com.penny.model.CreditCard;
import com.penny.model.Expense;
import com.penny.model.ExpenseSource;
import com.penny.model.Income;
import com.penny.model.IncomeSource;
import com.penny.model.RecurringExpense;
import com.penny.model.RecurringIncome;
import com.penny.model.RecurringRule;
import com.penny.repository.ExpenseRepository;
import com.penny.repository.IncomeRepository;
import com.penny.repository.RecurringExpenseRepository;
import com.penny.repository.RecurringIncomeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Optional;

@Service
public class RecurringEntryGenerator {

    private final RecurringExpenseRepository recurringExpenses;
    private final RecurringIncomeRepository recurringIncomes;
    private final ExpenseRepository expenses;
    private final IncomeRepository incomes;
    private final Clock clock;

    public RecurringEntryGenerator(
        RecurringExpenseRepository recurringExpenses,
        RecurringIncomeRepository recurringIncomes,
        ExpenseRepository expenses,
        IncomeRepository incomes,
        Clock clock
    ) {
        this.recurringExpenses = recurringExpenses;
        this.recurringIncomes = recurringIncomes;
        this.expenses = expenses;
        this.incomes = incomes;
        this.clock = clock;
    }

    @Transactional
    public int generateDueEntries() {
        int created = 0;
        for (RecurringExpense rule : recurringExpenses.findByActiveTrue()) {
            if (generate(rule)) {
                created++;
            }
        }
        for (RecurringIncome rule : recurringIncomes.findByActiveTrue()) {
            if (generate(rule)) {
                created++;
            }
        }
        return created;
    }

    @Transactional
    public boolean generate(RecurringExpense rule) {
        Optional<LocalDate> due = dueDate(rule);
        if (due.isEmpty()) {
            return false;
        }
        Expense expense = new Expense(rule.getUser(), ExpenseSource.RECURRING);
        expense.setCategory(rule.getCategory());
        expense.setAmount(rule.getAmount());
        expense.setDescription(rule.getDescription());
        expense.setOccurredOn(due.get());
        CreditCard card = rule.getCreditCard();
        expense.chargeTo(card, card == null ? null : CreditCardBilling.billingMonth(card, due.get()));
        expense.setRecurringExpense(rule);
        expenses.save(expense);
        markGenerated(rule, due.get());
        return true;
    }

    @Transactional
    public boolean generate(RecurringIncome rule) {
        Optional<LocalDate> due = dueDate(rule);
        if (due.isEmpty()) {
            return false;
        }
        Income income = new Income(rule.getUser(), IncomeSource.RECURRING);
        income.setCategory(rule.getCategory());
        income.setAmount(rule.getAmount());
        income.setDescription(rule.getDescription());
        income.setReceivedOn(due.get());
        income.setRecurringIncome(rule);
        incomes.save(income);
        markGenerated(rule, due.get());
        return true;
    }

    private Optional<LocalDate> dueDate(RecurringRule rule) {
        return rule.dueDateToGenerate(LocalDate.now(clock.withZone(rule.getUser().zoneId())));
    }

    private void markGenerated(RecurringRule rule, LocalDate due) {
        rule.setLastGeneratedMonth(YearMonth.from(due).atDay(1));
    }
}
