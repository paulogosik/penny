package com.penny.repository;

import com.penny.model.Goal;
import com.penny.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GoalRepository extends JpaRepository<Goal, UUID> {

    List<Goal> findByUserOrderByCreatedAtAsc(User user);

    Optional<Goal> findByIdAndUser(UUID id, User user);
}
