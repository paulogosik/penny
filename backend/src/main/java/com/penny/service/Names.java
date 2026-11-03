package com.penny.service;

import java.text.Normalizer;
import java.util.Locale;

final class Names {

    private Names() {
    }

    static String normalize(String value) {
        if (value == null) {
            return "";
        }
        String withoutAccents = Normalizer.normalize(value, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return withoutAccents.strip().toLowerCase(Locale.ROOT);
    }
}
