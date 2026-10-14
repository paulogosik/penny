package com.penny.service;

import java.time.LocalDate;
import java.util.List;

public record ClassificationContext(
    List<String> categories,
    String fallbackCategory,
    List<String> creditCards,
    LocalDate today
) {
}
