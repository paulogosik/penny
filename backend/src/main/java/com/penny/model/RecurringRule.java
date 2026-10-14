package com.penny.model;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Optional;

public interface RecurringRule {

    User getUser();

    Integer getDayOfMonth();

    LocalDate getStartsOn();

    LocalDate getEndsOn();

    boolean isActive();

    LocalDate getLastGeneratedMonth();

    void setLastGeneratedMonth(LocalDate lastGeneratedMonth);

    default Optional<LocalDate> dueDateToGenerate(LocalDate today) {
        if (!isActive()) {
            return Optional.empty();
        }
        YearMonth month = YearMonth.from(today);
        if (getLastGeneratedMonth() != null && !YearMonth.from(getLastGeneratedMonth()).isBefore(month)) {
            return Optional.empty();
        }
        LocalDate due = month.atDay(Math.min(getDayOfMonth(), month.lengthOfMonth()));
        boolean beforeStart = due.isBefore(getStartsOn());
        boolean afterEnd = getEndsOn() != null && due.isAfter(getEndsOn());
        if (due.isAfter(today) || beforeStart || afterEnd) {
            return Optional.empty();
        }
        return Optional.of(due);
    }
}
