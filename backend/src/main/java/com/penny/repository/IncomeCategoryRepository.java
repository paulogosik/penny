package com.penny.repository;

import com.penny.model.IncomeCategory;
import com.penny.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IncomeCategoryRepository extends JpaRepository<IncomeCategory, UUID> {

    List<IncomeCategory> findByUserAndArchivedAtIsNullOrderByName(User user);

    List<IncomeCategory> findByUserOrderByName(User user);

    Optional<IncomeCategory> findByIdAndUser(UUID id, User user);

    boolean existsByUserAndNameIgnoreCaseAndArchivedAtIsNull(User user, String name);

    boolean existsByUserAndNameIgnoreCaseAndArchivedAtIsNullAndIdNot(User user, String name, UUID id);
}
