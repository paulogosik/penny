package com.penny.service;

import com.penny.dto.ExpenseResponse;
import com.penny.dto.InstallmentPlanRequest;
import com.penny.dto.InstallmentPlanResponse;
import com.penny.exception.NotFoundException;
import com.penny.model.CreditCard;
import com.penny.model.Expense;
import com.penny.model.ExpenseCategory;
import com.penny.model.ExpenseSource;
import com.penny.model.InstallmentPlan;
import com.penny.model.User;
import com.penny.repository.ExpenseRepository;
import com.penny.repository.InstallmentPlanRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class InstallmentPlanService {

    private final InstallmentPlanRepository plans;
    private final ExpenseRepository expenses;
    private final EntityLookup lookup;

    public InstallmentPlanService(InstallmentPlanRepository plans, ExpenseRepository expenses, EntityLookup lookup) {
        this.plans = plans;
        this.expenses = expenses;
        this.lookup = lookup;
    }

    @Transactional
    public List<Expense> createPlan(
        User user,
        CreditCard card,
        ExpenseCategory category,
        String description,
        BigDecimal totalAmount,
        int installmentsCount,
        LocalDate purchasedOn,
        ExpenseSource source,
        String rawInput,
        String aiModel
    ) {
        List<BigDecimal> amounts = CreditCardBilling.splitInstallments(totalAmount, installmentsCount);
        InstallmentPlan plan = plans.save(new InstallmentPlan(
            user, card, category, description, totalAmount, installmentsCount, purchasedOn
        ));
        LocalDate firstBillingMonth = CreditCardBilling.billingMonth(card, purchasedOn);

        List<Expense> installments = new ArrayList<>(installmentsCount);
        for (int i = 0; i < installmentsCount; i++) {
            Expense expense = new Expense(user, source);
            expense.setCategory(category);
            expense.setAmount(amounts.get(i));
            expense.setDescription(description);
            expense.setOccurredOn(purchasedOn.plusMonths(i));
            expense.chargeTo(card, firstBillingMonth.plusMonths(i));
            expense.linkInstallment(plan, i + 1);
            expense.setRawInput(rawInput);
            expense.setAiModel(aiModel);
            installments.add(expense);
        }
        return expenses.saveAllAndFlush(installments);
    }

    @Transactional
    public InstallmentPlanResponse create(User user, InstallmentPlanRequest request) {
        CreditCard card = lookup.activeCreditCard(user, request.creditCardId());
        ExpenseCategory category = lookup.activeExpenseCategory(user, request.categoryId());
        List<Expense> installments = createPlan(
            user,
            card,
            category,
            request.description().strip(),
            request.totalAmount(),
            request.installmentsCount(),
            request.purchasedOn(),
            ExpenseSource.PANEL,
            null,
            null
        );
        InstallmentPlan plan = installments.getFirst().getInstallmentPlan();
        return InstallmentPlanResponse.from(plan, installments.stream().map(ExpenseResponse::from).toList());
    }

    @Transactional(readOnly = true)
    public List<InstallmentPlanResponse> list(User user) {
        List<InstallmentPlan> found = plans.findByUserOrderByPurchasedOnDesc(user);
        if (found.isEmpty()) {
            return List.of();
        }
        Map<UUID, List<ExpenseResponse>> installmentsByPlan = expenses
            .findByInstallmentPlanInOrderByInstallmentNumber(found)
            .stream()
            .collect(Collectors.groupingBy(
                expense -> expense.getInstallmentPlan().getId(),
                Collectors.mapping(ExpenseResponse::from, Collectors.toList())
            ));
        return found.stream()
            .map(plan -> InstallmentPlanResponse.from(plan, installmentsByPlan.getOrDefault(plan.getId(), List.of())))
            .toList();
    }

    @Transactional(readOnly = true)
    public InstallmentPlanResponse get(User user, UUID id) {
        InstallmentPlan plan = find(user, id);
        List<ExpenseResponse> installments = expenses.findByInstallmentPlanOrderByInstallmentNumber(plan)
            .stream()
            .map(ExpenseResponse::from)
            .toList();
        return InstallmentPlanResponse.from(plan, installments);
    }

    @Transactional
    public void delete(User user, UUID id) {
        plans.delete(find(user, id));
    }

    private InstallmentPlan find(User user, UUID id) {
        return plans.findByIdAndUser(id, user)
            .orElseThrow(() -> new NotFoundException("INSTALLMENT_PLAN_NOT_FOUND", "Parcelamento não encontrado."));
    }
}
