package com.penny.service;

import java.util.List;

public final class DefaultCategories {

    public static final List<String> EXPENSE = List.of(
        "Mercado", "Restaurante", "Transporte", "Moradia", "Saúde",
        "Lazer", "Educação", "Compras", "Serviços"
    );

    public static final String EXPENSE_FALLBACK = "Outros";

    public static final List<String> INCOME = List.of("Salário");

    private DefaultCategories() {
    }
}
