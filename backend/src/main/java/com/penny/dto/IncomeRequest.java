package com.penny.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record IncomeRequest(
    @NotNull UUID categoryId,
    @NotNull @Positive @Digits(integer = 10, fraction = 2) BigDecimal amount,
    @NotBlank @Size(max = 200) String description,
    @NotNull LocalDate receivedOn
) {
}
