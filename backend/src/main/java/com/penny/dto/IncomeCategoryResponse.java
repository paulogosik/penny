package com.penny.dto;

import com.penny.model.IncomeCategory;

import java.util.UUID;

public record IncomeCategoryResponse(UUID id, String name, String color, boolean archived) {

    public static IncomeCategoryResponse from(IncomeCategory category) {
        return new IncomeCategoryResponse(category.getId(), category.getName(), category.getColor(), category.isArchived());
    }
}
