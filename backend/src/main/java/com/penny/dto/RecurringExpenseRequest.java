package com.penny.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record RecurringExpenseRequest(
    @NotNull UUID categoryId,
    UUID creditCardId,
    @NotBlank @Size(max = 200) String description,
    @NotNull @Positive @Digits(integer = 10, fraction = 2) BigDecimal amount,
    @NotNull @Min(1) @Max(31) Integer dayOfMonth,
    @NotNull LocalDate startsOn,
    LocalDate endsOn,
    Boolean active
) {
}
