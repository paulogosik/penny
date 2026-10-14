package com.penny.support;

import com.penny.service.ClassificationContext;
import com.penny.service.ClassifiedExpense;
import com.penny.service.ExpenseClassifier;

import java.math.BigDecimal;
import java.time.LocalDate;

public class FakeExpenseClassifier implements ExpenseClassifier {

    private ClassifiedExpense next;
    private ClassificationContext lastContext;
    private String lastMimeType;

    public void willReturn(String category, String amount, LocalDate date, String description, int installments, String creditCard) {
        next = new ClassifiedExpense(category, new BigDecimal(amount), date, description, installments, creditCard, "fake-model");
    }

    public void willReturn(String category, String amount, LocalDate date, String description) {
        willReturn(category, amount, date, description, 1, null);
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
    public ClassifiedExpense classifyText(String text, ClassificationContext context) {
        lastContext = context;
        return result(context);
    }

    @Override
    public ClassifiedExpense classifyReceipt(byte[] image, String mimeType, ClassificationContext context) {
        lastContext = context;
        lastMimeType = mimeType;
        return result(context);
    }

    private ClassifiedExpense result(ClassificationContext context) {
        if (next != null) {
            return next;
        }
        return new ClassifiedExpense(context.fallbackCategory(), new BigDecimal("10.00"), context.today(), "Gasto", 1, null, "fake-model");
    }
}
