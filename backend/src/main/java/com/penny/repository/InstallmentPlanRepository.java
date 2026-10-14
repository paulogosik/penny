package com.penny.repository;

import com.penny.model.CreditCard;
import com.penny.model.ExpenseCategory;
import com.penny.model.InstallmentPlan;
import com.penny.model.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InstallmentPlanRepository extends JpaRepository<InstallmentPlan, UUID> {

    @EntityGraph(attributePaths = {"creditCard", "category"})
    List<InstallmentPlan> findByUserOrderByPurchasedOnDesc(User user);

    @EntityGraph(attributePaths = {"creditCard", "category"})
    Optional<InstallmentPlan> findByIdAndUser(UUID id, User user);

    boolean existsByCategory(ExpenseCategory category);

    boolean existsByCreditCard(CreditCard creditCard);
}
