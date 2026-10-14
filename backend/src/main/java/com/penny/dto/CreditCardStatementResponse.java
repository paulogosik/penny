package com.penny.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record CreditCardStatementResponse(
    UUID creditCardId,
    String name,
    LocalDate billingMonth,
    LocalDate dueDate,
    BigDecimal total,
    List<ExpenseResponse> expenses
) {
}
