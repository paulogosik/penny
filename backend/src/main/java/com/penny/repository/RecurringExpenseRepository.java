package com.penny.repository;

import com.penny.model.CreditCard;
import com.penny.model.ExpenseCategory;
import com.penny.model.RecurringExpense;
import com.penny.model.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RecurringExpenseRepository extends JpaRepository<RecurringExpense, UUID> {

    @EntityGraph(attributePaths = {"category", "creditCard"})
    List<RecurringExpense> findByUserOrderByDayOfMonthAscDescriptionAsc(User user);

    @EntityGraph(attributePaths = {"category", "creditCard"})
    Optional<RecurringExpense> findByIdAndUser(UUID id, User user);

    @EntityGraph(attributePaths = {"user", "category", "creditCard"})
    List<RecurringExpense> findByActiveTrue();

    boolean existsByCategory(ExpenseCategory category);

    boolean existsByCreditCard(CreditCard creditCard);
}
