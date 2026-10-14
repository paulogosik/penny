package com.penny.service;

import com.penny.dto.RecurringIncomeRequest;
import com.penny.dto.RecurringIncomeResponse;
import com.penny.exception.BusinessRuleException;
import com.penny.exception.NotFoundException;
import com.penny.model.RecurringIncome;
import com.penny.model.User;
import com.penny.repository.RecurringIncomeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class RecurringIncomeService {

    private final RecurringIncomeRepository recurringIncomes;
    private final RecurringEntryGenerator generator;
    private final EntityLookup lookup;

    public RecurringIncomeService(
        RecurringIncomeRepository recurringIncomes,
        RecurringEntryGenerator generator,
        EntityLookup lookup
    ) {
        this.recurringIncomes = recurringIncomes;
        this.generator = generator;
        this.lookup = lookup;
    }

    @Transactional(readOnly = true)
    public List<RecurringIncomeResponse> list(User user) {
        return recurringIncomes.findByUserOrderByDayOfMonthAscDescriptionAsc(user)
            .stream()
            .map(RecurringIncomeResponse::from)
            .toList();
    }

    @Transactional
    public RecurringIncomeResponse create(User user, RecurringIncomeRequest request) {
        RecurringIncome rule = new RecurringIncome(user);
        apply(user, rule, request);
        recurringIncomes.saveAndFlush(rule);
        generator.generate(rule);
        return RecurringIncomeResponse.from(rule);
    }

    @Transactional
    public RecurringIncomeResponse update(User user, UUID id, RecurringIncomeRequest request) {
        RecurringIncome rule = find(user, id);
        apply(user, rule, request);
        generator.generate(rule);
        return RecurringIncomeResponse.from(rule);
    }

    @Transactional
    public void delete(User user, UUID id) {
        recurringIncomes.delete(find(user, id));
    }

    private void apply(User user, RecurringIncome rule, RecurringIncomeRequest request) {
        if (request.endsOn() != null && request.endsOn().isBefore(request.startsOn())) {
            throw new BusinessRuleException("INVALID_PERIOD", "A data de fim não pode ser anterior à de início.");
        }
        rule.setCategory(lookup.activeIncomeCategory(user, request.categoryId()));
        rule.setDescription(request.description().strip());
        rule.setAmount(request.amount());
        rule.setDayOfMonth(request.dayOfMonth());
        rule.setStartsOn(request.startsOn());
        rule.setEndsOn(request.endsOn());
        rule.setActive(request.active() == null || request.active());
    }

    private RecurringIncome find(User user, UUID id) {
        return recurringIncomes.findByIdAndUser(id, user)
            .orElseThrow(() -> new NotFoundException("RECURRING_INCOME_NOT_FOUND", "Renda fixa não encontrada."));
    }
}
