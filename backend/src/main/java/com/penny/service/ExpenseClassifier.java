package com.penny.service;

public interface ExpenseClassifier {

    ClassifiedMessage classifyText(String text, ClassificationContext context);

    ClassifiedExpense classifyReceipt(byte[] image, String mimeType, ClassificationContext context);
}
