package com.penny.repository;

import com.penny.model.IncomeCategory;
import com.penny.model.RecurringIncome;
import com.penny.model.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RecurringIncomeRepository extends JpaRepository<RecurringIncome, UUID> {

    @EntityGraph(attributePaths = {"category"})
    List<RecurringIncome> findByUserOrderByDayOfMonthAscDescriptionAsc(User user);

    @EntityGraph(attributePaths = {"category"})
    Optional<RecurringIncome> findByIdAndUser(UUID id, User user);

    @EntityGraph(attributePaths = {"user", "category"})
    List<RecurringIncome> findByActiveTrue();

    boolean existsByCategory(IncomeCategory category);
}
