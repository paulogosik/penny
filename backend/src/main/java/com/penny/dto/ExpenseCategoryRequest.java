package com.penny.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ExpenseCategoryRequest(
    @NotBlank @Size(max = 50) String name,
    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$") String color,
    @PositiveOrZero @Digits(integer = 10, fraction = 2) BigDecimal monthlyBudget
) {
}
