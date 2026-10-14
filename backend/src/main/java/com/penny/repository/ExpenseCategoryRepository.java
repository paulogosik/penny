package com.penny.repository;

import com.penny.model.ExpenseCategory;
import com.penny.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ExpenseCategoryRepository extends JpaRepository<ExpenseCategory, UUID> {

    List<ExpenseCategory> findByUserAndArchivedAtIsNullOrderByName(User user);

    List<ExpenseCategory> findByUserOrderByName(User user);

    Optional<ExpenseCategory> findByIdAndUser(UUID id, User user);

    Optional<ExpenseCategory> findByUserAndFallbackTrue(User user);

    boolean existsByUserAndNameIgnoreCaseAndArchivedAtIsNull(User user, String name);

    boolean existsByUserAndNameIgnoreCaseAndArchivedAtIsNullAndIdNot(User user, String name, UUID id);
}
