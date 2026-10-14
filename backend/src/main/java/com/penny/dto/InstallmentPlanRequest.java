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

public record InstallmentPlanRequest(
    @NotNull UUID creditCardId,
    @NotNull UUID categoryId,
    @NotBlank @Size(max = 200) String description,
    @NotNull @Positive @Digits(integer = 10, fraction = 2) BigDecimal totalAmount,
    @NotNull @Min(2) @Max(99) Integer installmentsCount,
    @NotNull LocalDate purchasedOn
) {
}
