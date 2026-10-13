package com.penny.dto;

public record ExpenseResponse(
    String category,
    double amount,
    String date,
    String description
) {
}
