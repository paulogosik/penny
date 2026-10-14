package com.penny.model;

import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

public enum IncomeSource {

    PANEL("panel"),
    RECURRING("recurring");

    private final String value;

    IncomeSource(String value) {
        this.value = value;
    }

    @JsonValue
    public String value() {
        return value;
    }

    public static IncomeSource fromValue(String value) {
        return Arrays.stream(values())
            .filter(source -> source.value.equals(value))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Origem de renda desconhecida: " + value));
    }
}
