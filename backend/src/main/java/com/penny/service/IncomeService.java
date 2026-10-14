package com.penny.service;

import com.penny.dto.IncomeRequest;
import com.penny.dto.IncomeResponse;
import com.penny.exception.NotFoundException;
import com.penny.model.Income;
import com.penny.model.IncomeSource;
import com.penny.model.User;
import com.penny.repository.IncomeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

@Service
public class IncomeService {

    private final IncomeRepository incomes;
    private final EntityLookup lookup;
    private final Clock clock;

    public IncomeService(IncomeRepository incomes, EntityLookup lookup, Clock clock) {
        this.incomes = incomes;
        this.lookup = lookup;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<IncomeResponse> list(User user, LocalDate from, LocalDate to) {
        YearMonth currentMonth = YearMonth.now(clock.withZone(user.zoneId()));
        LocalDate start = from == null ? currentMonth.atDay(1) : from;
        LocalDate end = to == null ? currentMonth.atEndOfMonth() : to;
        return incomes.findByUserAndReceivedOnBetweenOrderByReceivedOnDesc(user, start, end)
            .stream()
            .map(IncomeResponse::from)
            .toList();
    }

    @Transactional(readOnly = true)
    public IncomeResponse get(User user, UUID id) {
        return IncomeResponse.from(find(user, id));
    }

    @Transactional
    public IncomeResponse create(User user, IncomeRequest request) {
        Income income = new Income(user, IncomeSource.PANEL);
        apply(user, income, request);
        return IncomeResponse.from(incomes.saveAndFlush(income));
    }

    @Transactional
    public IncomeResponse update(User user, UUID id, IncomeRequest request) {
        Income income = find(user, id);
        apply(user, income, request);
        return IncomeResponse.from(incomes.saveAndFlush(income));
    }

    @Transactional
    public void delete(User user, UUID id) {
        incomes.delete(find(user, id));
    }

    private void apply(User user, Income income, IncomeRequest request) {
        income.setCategory(lookup.activeIncomeCategory(user, request.categoryId()));
        income.setAmount(request.amount());
        income.setDescription(request.description().strip());
        income.setReceivedOn(request.receivedOn());
    }

    private Income find(User user, UUID id) {
        return incomes.findByIdAndUser(id, user)
            .orElseThrow(() -> new NotFoundException("INCOME_NOT_FOUND", "Renda não encontrada."));
    }
}
