package com.penny.support;

import com.penny.service.ClassificationContext;
import com.penny.service.ClassifiedExpense;
import com.penny.service.ClassifiedMessage;
import com.penny.service.ExpenseClassifier;

import java.math.BigDecimal;
import java.time.LocalDate;

public class FakeExpenseClassifier implements ExpenseClassifier {

    private ClassifiedMessage next;
    private ClassificationContext lastContext;
    private String lastMimeType;

    public void willReturn(String category, String amount, LocalDate date, String description, int installments, String creditCard) {
        next = new ClassifiedMessage.ExpenseMessage(
            new ClassifiedExpense(category, new BigDecimal(amount), date, description, installments, creditCard, "fake-model")
        );
    }

    public void willReturn(String category, String amount, LocalDate date, String description) {
        willReturn(category, amount, date, description, 1, null);
    }

    public void willReturnCreditCard(String name, Integer closingDay, Integer dueDay, String creditLimit) {
        next = new ClassifiedMessage.CreditCardMessage(
            name,
            closingDay,
            dueDay,
            creditLimit == null ? null : new BigDecimal(creditLimit)
        );
    }

    public void willReturnUnrecognized() {
        next = new ClassifiedMessage.Unrecognized();
    }

    public ClassificationContext lastContext() {
        return lastContext;
    }

    public String lastMimeType() {
        return lastMimeType;
    }

    public void reset() {
        next = null;
        lastContext = null;
        lastMimeType = null;
    }

    @Override
    public ClassifiedMessage classifyText(String text, ClassificationContext context) {
        lastContext = context;
        return next != null ? next : new ClassifiedMessage.ExpenseMessage(defaultExpense(context));
    }

    @Override
    public ClassifiedExpense classifyReceipt(byte[] image, String mimeType, ClassificationContext context) {
        lastContext = context;
        lastMimeType = mimeType;
        if (next instanceof ClassifiedMessage.ExpenseMessage message) {
            return message.expense();
        }
        return defaultExpense(context);
    }

    private ClassifiedExpense defaultExpense(ClassificationContext context) {
        return new ClassifiedExpense(context.fallbackCategory(), new BigDecimal("10.00"), context.today(), "Gasto", 1, null, "fake-model");
    }
}
