package com.penny.dto;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.UUID;

public record ExpenseFilter(
    LocalDate from,
    LocalDate to,
    UUID categoryId,
    UUID creditCardId,
    YearMonth billingMonth
) {
}
