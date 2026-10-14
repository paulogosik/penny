package com.penny.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record GoalRequest(
    @NotBlank @Size(max = 100) String name,
    @NotNull @Positive @Digits(integer = 10, fraction = 2) BigDecimal targetAmount,
    @PositiveOrZero @Digits(integer = 10, fraction = 2) BigDecimal savedAmount,
    LocalDate targetDate
) {
}
