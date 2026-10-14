package com.penny.model;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class ExpenseSourceConverter implements AttributeConverter<ExpenseSource, String> {

    @Override
    public String convertToDatabaseColumn(ExpenseSource source) {
        return source == null ? null : source.value();
    }

    @Override
    public ExpenseSource convertToEntityAttribute(String value) {
        return value == null ? null : ExpenseSource.fromValue(value);
    }
}
