package com.penny.repository;

import com.penny.model.Expense;
import com.penny.model.User;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.UUID;

public final class ExpenseSpecifications {

    private ExpenseSpecifications() {
    }

    public static Specification<Expense> belongsTo(User user) {
        return (root, query, cb) -> cb.equal(root.get("user"), user);
    }

    public static Specification<Expense> occurredFrom(LocalDate from) {
        return from == null ? null : (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("occurredOn"), from);
    }

    public static Specification<Expense> occurredUntil(LocalDate to) {
        return to == null ? null : (root, query, cb) -> cb.lessThanOrEqualTo(root.get("occurredOn"), to);
    }

    public static Specification<Expense> inCategory(UUID categoryId) {
        return categoryId == null ? null : (root, query, cb) -> cb.equal(root.get("category").get("id"), categoryId);
    }

    public static Specification<Expense> chargedTo(UUID creditCardId) {
        return creditCardId == null ? null : (root, query, cb) -> cb.equal(root.get("creditCard").get("id"), creditCardId);
    }

    public static Specification<Expense> billedIn(LocalDate billingMonth) {
        return billingMonth == null ? null : (root, query, cb) -> cb.equal(root.get("billingMonth"), billingMonth);
    }
}
