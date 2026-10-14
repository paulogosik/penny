package com.penny.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TextExpenseRequest(@NotBlank @Size(max = 1000) String text) {
}
