package com.penny.service;

import com.penny.exception.BusinessRuleException;
import com.penny.exception.NotFoundException;
import com.penny.model.CreditCard;
import com.penny.model.ExpenseCategory;
import com.penny.model.IncomeCategory;
import com.penny.model.User;
import com.penny.repository.CreditCardRepository;
import com.penny.repository.ExpenseCategoryRepository;
import com.penny.repository.IncomeCategoryRepository;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class EntityLookup {

    private final ExpenseCategoryRepository expenseCategories;
    private final IncomeCategoryRepository incomeCategories;
    private final CreditCardRepository creditCards;

    public EntityLookup(
        ExpenseCategoryRepository expenseCategories,
        IncomeCategoryRepository incomeCategories,
        CreditCardRepository creditCards
    ) {
        this.expenseCategories = expenseCategories;
        this.incomeCategories = incomeCategories;
        this.creditCards = creditCards;
    }

    public ExpenseCategory expenseCategory(User user, UUID id) {
        return expenseCategories.findByIdAndUser(id, user)
            .orElseThrow(() -> new NotFoundException("CATEGORY_NOT_FOUND", "Categoria não encontrada."));
    }

    public ExpenseCategory activeExpenseCategory(User user, UUID id) {
        ExpenseCategory category = expenseCategory(user, id);
        if (category.isArchived()) {
            throw new BusinessRuleException("CATEGORY_ARCHIVED", "Essa categoria foi removida e não pode receber lançamentos.");
        }
        return category;
    }

    public IncomeCategory incomeCategory(User user, UUID id) {
        return incomeCategories.findByIdAndUser(id, user)
            .orElseThrow(() -> new NotFoundException("CATEGORY_NOT_FOUND", "Categoria não encontrada."));
    }

    public IncomeCategory activeIncomeCategory(User user, UUID id) {
        IncomeCategory category = incomeCategory(user, id);
        if (category.isArchived()) {
            throw new BusinessRuleException("CATEGORY_ARCHIVED", "Essa categoria foi removida e não pode receber lançamentos.");
        }
        return category;
    }

    public CreditCard creditCard(User user, UUID id) {
        return creditCards.findByIdAndUser(id, user)
            .orElseThrow(() -> new NotFoundException("CREDIT_CARD_NOT_FOUND", "Cartão não encontrado."));
    }

    public CreditCard activeCreditCard(User user, UUID id) {
        CreditCard card = creditCard(user, id);
        if (card.isArchived()) {
            throw new BusinessRuleException("CREDIT_CARD_ARCHIVED", "Esse cartão foi removido e não pode receber lançamentos.");
        }
        return card;
    }

    public CreditCard optionalActiveCreditCard(User user, UUID id) {
        return id == null ? null : activeCreditCard(user, id);
    }
}
