package com.penny.service;

import com.penny.dto.CreditCardRequest;
import com.penny.dto.CreditCardResponse;
import com.penny.dto.CreditCardStatementResponse;
import com.penny.dto.ExpenseResponse;
import com.penny.exception.BusinessRuleException;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class CreditCardService {

    public record Registration(CreditCardResponse card, boolean created) {
    }

    private static final int MAX_NAME_LENGTH = 50;

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
    public Registration registerFromMessage(User user, ClassifiedMessage.CreditCardMessage message) {
        String name = message.name() == null ? "" : message.name().strip();
        ensureComplete(name, message.closingDay(), message.dueDay());
        if (!isDayOfMonth(message.closingDay()) || !isDayOfMonth(message.dueDay())) {
            throw new BusinessRuleException(
                "INVALID_CREDIT_CARD_DAY",
                "Os dias de fechamento e de vencimento precisam estar entre 1 e 31."
            );
        }
        BigDecimal creditLimit = message.creditLimit() == null || message.creditLimit().signum() < 0 ? null : message.creditLimit();
        if (creditLimit != null && creditLimit.compareTo(ExpenseService.MAX_AMOUNT) > 0) {
            throw new BusinessRuleException("AMOUNT_TOO_LARGE", "Esse limite é alto demais para ser registrado.");
        }
        String cardName = name.length() <= MAX_NAME_LENGTH ? name : name.substring(0, MAX_NAME_LENGTH).strip();

        Optional<CreditCard> existing = CreditCardNames.sameName(cards.findByUserAndArchivedAtIsNullOrderByName(user), cardName);
        if (existing.isPresent()) {
            CreditCard card = existing.get();
            card.setClosingDay(message.closingDay());
            card.setDueDay(message.dueDay());
            if (creditLimit != null) {
                card.setCreditLimit(creditLimit);
            }
            return new Registration(CreditCardResponse.from(card), false);
        }
        CreditCard card = new CreditCard(user, cardName, message.closingDay(), message.dueDay());
        card.setCreditLimit(creditLimit);
        return new Registration(CreditCardResponse.from(cards.save(card)), true);
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

    private static void ensureComplete(String name, Integer closingDay, Integer dueDay) {
        List<String> missing = new ArrayList<>();
        if (name.isEmpty()) {
            missing.add("o nome do cartão");
        }
        if (closingDay == null) {
            missing.add("o dia de fechamento");
        }
        if (dueDay == null) {
            missing.add("o dia de vencimento");
        }
        if (missing.isEmpty()) {
            return;
        }
        String listed = missing.size() == 1
            ? missing.getFirst()
            : String.join(", ", missing.subList(0, missing.size() - 1)) + " e " + missing.getLast();
        String example = name.isEmpty() ? "Nubank" : name;
        throw new BusinessRuleException(
            "CREDIT_CARD_DETAILS_MISSING",
            "Faltou " + listed + ". Tente: \"meu cartão " + example + " fecha dia 5 e vence dia 12\"."
        );
    }

    private static boolean isDayOfMonth(int day) {
        return day >= 1 && day <= 31;
    }

    private ConflictException nameTaken() {
        return new ConflictException("CREDIT_CARD_NAME_TAKEN", "Já existe um cartão com esse nome.");
    }
}
