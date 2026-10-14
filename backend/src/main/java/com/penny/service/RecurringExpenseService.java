package com.penny.service;

import com.penny.dto.RecurringExpenseRequest;
import com.penny.dto.RecurringExpenseResponse;
import com.penny.exception.BusinessRuleException;
import com.penny.exception.NotFoundException;
import com.penny.model.RecurringExpense;
import com.penny.model.User;
import com.penny.repository.RecurringExpenseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class RecurringExpenseService {

    private final RecurringExpenseRepository recurringExpenses;
    private final RecurringEntryGenerator generator;
    private final EntityLookup lookup;

    public RecurringExpenseService(
        RecurringExpenseRepository recurringExpenses,
        RecurringEntryGenerator generator,
        EntityLookup lookup
    ) {
        this.recurringExpenses = recurringExpenses;
        this.generator = generator;
        this.lookup = lookup;
    }

    @Transactional(readOnly = true)
    public List<RecurringExpenseResponse> list(User user) {
        return recurringExpenses.findByUserOrderByDayOfMonthAscDescriptionAsc(user)
            .stream()
            .map(RecurringExpenseResponse::from)
            .toList();
    }

    @Transactional
    public RecurringExpenseResponse create(User user, RecurringExpenseRequest request) {
        RecurringExpense rule = new RecurringExpense(user);
        apply(user, rule, request);
        recurringExpenses.saveAndFlush(rule);
        generator.generate(rule);
        return RecurringExpenseResponse.from(rule);
    }

    @Transactional
    public RecurringExpenseResponse update(User user, UUID id, RecurringExpenseRequest request) {
        RecurringExpense rule = find(user, id);
        apply(user, rule, request);
        generator.generate(rule);
        return RecurringExpenseResponse.from(rule);
    }

    @Transactional
    public void delete(User user, UUID id) {
        recurringExpenses.delete(find(user, id));
    }

    private void apply(User user, RecurringExpense rule, RecurringExpenseRequest request) {
        if (request.endsOn() != null && request.endsOn().isBefore(request.startsOn())) {
            throw new BusinessRuleException("INVALID_PERIOD", "A data de fim não pode ser anterior à de início.");
        }
        rule.setCategory(lookup.activeExpenseCategory(user, request.categoryId()));
        rule.setCreditCard(lookup.optionalActiveCreditCard(user, request.creditCardId()));
        rule.setDescription(request.description().strip());
        rule.setAmount(request.amount());
        rule.setDayOfMonth(request.dayOfMonth());
        rule.setStartsOn(request.startsOn());
        rule.setEndsOn(request.endsOn());
        rule.setActive(request.active() == null || request.active());
    }

    private RecurringExpense find(User user, UUID id) {
        return recurringExpenses.findByIdAndUser(id, user)
            .orElseThrow(() -> new NotFoundException("RECURRING_EXPENSE_NOT_FOUND", "Gasto fixo não encontrado."));
    }
}
