package com.penny.service;

import com.penny.exception.BusinessRuleException;
import com.penny.model.User;

import java.util.regex.Pattern;

public final class PhoneNumbers {

    private static final Pattern E164 = Pattern.compile("^\\+[1-9]\\d{7,14}$");

    private PhoneNumbers() {
    }

    public static void ensurePresent(User user) {
        if (!user.hasPhoneNumber()) {
            throw new BusinessRuleException(
                "PHONE_NUMBER_REQUIRED",
                "Compartilhe seu número de telefone para começar a registrar gastos."
            );
        }
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
