package com.penny.service;

import com.penny.dto.IncomeCategoryRequest;
import com.penny.dto.IncomeCategoryResponse;
import com.penny.exception.ConflictException;
import com.penny.model.IncomeCategory;
import com.penny.model.User;
import com.penny.repository.IncomeCategoryRepository;
import com.penny.repository.IncomeRepository;
import com.penny.repository.RecurringIncomeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

@Service
public class IncomeCategoryService {

    private final IncomeCategoryRepository categories;
    private final IncomeRepository incomes;
    private final RecurringIncomeRepository recurringIncomes;
    private final EntityLookup lookup;
    private final Clock clock;

    public IncomeCategoryService(
        IncomeCategoryRepository categories,
        IncomeRepository incomes,
        RecurringIncomeRepository recurringIncomes,
        EntityLookup lookup,
        Clock clock
    ) {
        this.categories = categories;
        this.incomes = incomes;
        this.recurringIncomes = recurringIncomes;
        this.lookup = lookup;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<IncomeCategoryResponse> list(User user, boolean includeArchived) {
        List<IncomeCategory> found = includeArchived
            ? categories.findByUserOrderByName(user)
            : categories.findByUserAndArchivedAtIsNullOrderByName(user);
        return found.stream().map(IncomeCategoryResponse::from).toList();
    }

    @Transactional
    public IncomeCategoryResponse create(User user, IncomeCategoryRequest request) {
        String name = request.name().strip();
        if (categories.existsByUserAndNameIgnoreCaseAndArchivedAtIsNull(user, name)) {
            throw nameTaken();
        }
        IncomeCategory category = new IncomeCategory(user, name);
        category.setColor(request.color());
        return IncomeCategoryResponse.from(categories.save(category));
    }

    @Transactional
    public IncomeCategoryResponse update(User user, UUID id, IncomeCategoryRequest request) {
        IncomeCategory category = lookup.activeIncomeCategory(user, id);
        String name = request.name().strip();
        if (categories.existsByUserAndNameIgnoreCaseAndArchivedAtIsNullAndIdNot(user, name, id)) {
            throw nameTaken();
        }
        category.setName(name);
        category.setColor(request.color());
        return IncomeCategoryResponse.from(category);
    }

    @Transactional
    public void delete(User user, UUID id) {
        IncomeCategory category = lookup.incomeCategory(user, id);
        if (category.isArchived()) {
            return;
        }
        if (incomes.existsByCategory(category) || recurringIncomes.existsByCategory(category)) {
            category.archive(clock.instant());
        } else {
            categories.delete(category);
        }
    }

    private ConflictException nameTaken() {
        return new ConflictException("CATEGORY_NAME_TAKEN", "Já existe uma categoria com esse nome.");
    }
}
