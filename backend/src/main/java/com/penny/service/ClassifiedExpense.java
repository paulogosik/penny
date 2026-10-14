package com.penny.service;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ClassifiedExpense(
    String category,
    BigDecimal amount,
    LocalDate date,
    String description,
    int installments,
    String creditCard,
    String model
) {
}
