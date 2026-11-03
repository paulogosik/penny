package com.penny.service;

import java.math.BigDecimal;

public sealed interface ClassifiedMessage {

    record ExpenseMessage(ClassifiedExpense expense) implements ClassifiedMessage {
    }

    record CreditCardMessage(String name, Integer closingDay, Integer dueDay, BigDecimal creditLimit) implements ClassifiedMessage {
    }

    record Unrecognized() implements ClassifiedMessage {
    }
}
