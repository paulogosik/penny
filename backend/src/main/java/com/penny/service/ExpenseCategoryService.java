package com.penny.service;

import com.penny.dto.ExpenseCategoryRequest;
import com.penny.dto.ExpenseCategoryResponse;
import com.penny.exception.BusinessRuleException;
import com.penny.exception.ConflictException;
import com.penny.model.ExpenseCategory;
import com.penny.model.User;
import com.penny.repository.ExpenseCategoryRepository;
import com.penny.repository.ExpenseRepository;
import com.penny.repository.InstallmentPlanRepository;
import com.penny.repository.RecurringExpenseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

@Service
public class ExpenseCategoryService {

    private final ExpenseCategoryRepository categories;
    private final ExpenseRepository expenses;
    private final InstallmentPlanRepository installmentPlans;
    private final RecurringExpenseRepository recurringExpenses;
    private final EntityLookup lookup;
    private final Clock clock;

    public ExpenseCategoryService(
        ExpenseCategoryRepository categories,
        ExpenseRepository expenses,
        InstallmentPlanRepository installmentPlans,
        RecurringExpenseRepository recurringExpenses,
        EntityLookup lookup,
        Clock clock
    ) {
        this.categories = categories;
        this.expenses = expenses;
        this.installmentPlans = installmentPlans;
        this.recurringExpenses = recurringExpenses;
        this.lookup = lookup;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<ExpenseCategoryResponse> list(User user, boolean includeArchived) {
        List<ExpenseCategory> found = includeArchived
            ? categories.findByUserOrderByName(user)
            : categories.findByUserAndArchivedAtIsNullOrderByName(user);
        return found.stream().map(ExpenseCategoryResponse::from).toList();
    }

    @Transactional
    public ExpenseCategoryResponse create(User user, ExpenseCategoryRequest request) {
        String name = request.name().strip();
        if (categories.existsByUserAndNameIgnoreCaseAndArchivedAtIsNull(user, name)) {
            throw nameTaken();
        }
        ExpenseCategory category = new ExpenseCategory(user, name, false);
        category.setColor(request.color());
        category.setMonthlyBudget(request.monthlyBudget());
        return ExpenseCategoryResponse.from(categories.save(category));
    }

    @Transactional
    public ExpenseCategoryResponse update(User user, UUID id, ExpenseCategoryRequest request) {
        ExpenseCategory category = lookup.activeExpenseCategory(user, id);
        String name = request.name().strip();
        if (categories.existsByUserAndNameIgnoreCaseAndArchivedAtIsNullAndIdNot(user, name, id)) {
            throw nameTaken();
        }
        category.setName(name);
        category.setColor(request.color());
        category.setMonthlyBudget(request.monthlyBudget());
        return ExpenseCategoryResponse.from(category);
    }

    @Transactional
    public void delete(User user, UUID id) {
        ExpenseCategory category = lookup.expenseCategory(user, id);
        if (category.isFallback()) {
            throw new BusinessRuleException(
                "FALLBACK_CATEGORY_PROTECTED",
                "A categoria \"" + category.getName() + "\" é usada quando a IA não encontra outra e não pode ser removida."
            );
        }
        if (category.isArchived()) {
            return;
        }
        boolean inUse = expenses.existsByCategory(category)
            || installmentPlans.existsByCategory(category)
            || recurringExpenses.existsByCategory(category);
        if (inUse) {
            category.archive(clock.instant());
        } else {
            categories.delete(category);
        }
    }

    private ConflictException nameTaken() {
        return new ConflictException("CATEGORY_NAME_TAKEN", "Já existe uma categoria com esse nome.");
    }
}
