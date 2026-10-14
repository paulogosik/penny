package com.penny.service;

import com.penny.dto.CreditCardRequest;
import com.penny.dto.CreditCardResponse;
import com.penny.dto.CreditCardStatementResponse;
import com.penny.dto.ExpenseResponse;
import com.penny.exception.ConflictException;
import com.penny.model.CreditCard;
import com.penny.model.Expense;
import com.penny.model.User;
import com.penny.repository.CreditCardRepository;
import com.penny.repository.ExpenseRepository;
import com.penny.repository.InstallmentPlanRepository;
import com.penny.repository.RecurringExpenseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

@Service
public class CreditCardService {

    private final CreditCardRepository cards;
    private final ExpenseRepository expenses;
    private final InstallmentPlanRepository installmentPlans;
    private final RecurringExpenseRepository recurringExpenses;
    private final EntityLookup lookup;
    private final Clock clock;

    public CreditCardService(
        CreditCardRepository cards,
        ExpenseRepository expenses,
        InstallmentPlanRepository installmentPlans,
        RecurringExpenseRepository recurringExpenses,
        EntityLookup lookup,
        Clock clock
    ) {
        this.cards = cards;
        this.expenses = expenses;
        this.installmentPlans = installmentPlans;
        this.recurringExpenses = recurringExpenses;
        this.lookup = lookup;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<CreditCardResponse> list(User user, boolean includeArchived) {
        List<CreditCard> found = includeArchived
            ? cards.findByUserOrderByName(user)
            : cards.findByUserAndArchivedAtIsNullOrderByName(user);
        return found.stream().map(CreditCardResponse::from).toList();
    }

    @Transactional
    public CreditCardResponse create(User user, CreditCardRequest request) {
        String name = request.name().strip();
        if (cards.existsByUserAndNameIgnoreCaseAndArchivedAtIsNull(user, name)) {
            throw nameTaken();
        }
        CreditCard card = new CreditCard(user, name, request.closingDay(), request.dueDay());
        card.setCreditLimit(request.creditLimit());
        return CreditCardResponse.from(cards.save(card));
    }

    @Transactional
    public CreditCardResponse update(User user, UUID id, CreditCardRequest request) {
        CreditCard card = lookup.activeCreditCard(user, id);
        String name = request.name().strip();
        if (cards.existsByUserAndNameIgnoreCaseAndArchivedAtIsNullAndIdNot(user, name, id)) {
            throw nameTaken();
        }
        card.setName(name);
        card.setClosingDay(request.closingDay());
        card.setDueDay(request.dueDay());
        card.setCreditLimit(request.creditLimit());
        return CreditCardResponse.from(card);
    }

    @Transactional
    public void delete(User user, UUID id) {
        CreditCard card = lookup.creditCard(user, id);
        if (card.isArchived()) {
            return;
        }
        boolean inUse = expenses.existsByCreditCard(card)
            || installmentPlans.existsByCreditCard(card)
            || recurringExpenses.existsByCreditCard(card);
        if (inUse) {
            card.archive(clock.instant());
        } else {
            cards.delete(card);
        }
    }

    @Transactional(readOnly = true)
    public CreditCardStatementResponse statement(User user, UUID id, YearMonth month) {
        CreditCard card = lookup.creditCard(user, id);
        LocalDate billingMonth = month.atDay(1);
        List<Expense> found = expenses.findByUserAndCreditCardAndBillingMonthOrderByOccurredOnAsc(user, card, billingMonth);
        BigDecimal total = found.stream().map(Expense::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new CreditCardStatementResponse(
            card.getId(),
            card.getName(),
            billingMonth,
            CreditCardBilling.dueDate(card.getDueDay(), billingMonth),
            total,
            found.stream().map(ExpenseResponse::from).toList()
        );
    }

    private ConflictException nameTaken() {
        return new ConflictException("CREDIT_CARD_NAME_TAKEN", "Já existe um cartão com esse nome.");
    }
}
