package com.penny.model;

import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

public enum ExpenseSource {

    TELEGRAM_TEXT("telegram_text"),
    TELEGRAM_PHOTO("telegram_photo"),
    PANEL("panel"),
    RECURRING("recurring");

    private final String value;

    ExpenseSource(String value) {
        this.value = value;
    }

    @JsonValue
    public String value() {
        return value;
    }

    public static ExpenseSource fromValue(String value) {
        return Arrays.stream(values())
            .filter(source -> source.value.equals(value))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Origem de gasto desconhecida: " + value));
    }
}
