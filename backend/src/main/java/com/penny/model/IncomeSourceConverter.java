package com.penny.model;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class IncomeSourceConverter implements AttributeConverter<IncomeSource, String> {

    @Override
    public String convertToDatabaseColumn(IncomeSource source) {
        return source == null ? null : source.value();
    }

    @Override
    public IncomeSource convertToEntityAttribute(String value) {
        return value == null ? null : IncomeSource.fromValue(value);
    }
}
