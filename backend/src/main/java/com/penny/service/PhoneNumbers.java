package com.penny.service;

import com.penny.exception.BusinessRuleException;

import java.util.regex.Pattern;

public final class PhoneNumbers {

    private static final Pattern E164 = Pattern.compile("^\\+[1-9]\\d{7,14}$");

    private PhoneNumbers() {
    }

    public static String normalize(String raw) {
        String candidate = "+" + (raw == null ? "" : raw.replaceAll("\\D", ""));
        if (!E164.matcher(candidate).matches()) {
            throw new BusinessRuleException(
                "INVALID_PHONE_NUMBER",
                "Número de telefone inválido. Use o formato internacional, com código do país e DDD."
            );
        }
        return candidate;
    }
}
