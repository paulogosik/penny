package com.penny.repository;

import com.penny.model.Income;
import com.penny.model.IncomeCategory;
import com.penny.model.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IncomeRepository extends JpaRepository<Income, UUID> {

    @EntityGraph(attributePaths = {"category"})
    List<Income> findByUserAndReceivedOnBetweenOrderByReceivedOnDesc(User user, LocalDate start, LocalDate end);

    @EntityGraph(attributePaths = {"category"})
    Optional<Income> findByIdAndUser(UUID id, User user);

    boolean existsByCategory(IncomeCategory category);

    @Query("""
        select coalesce(sum(i.amount), 0)
        from Income i
        where i.user = :user and i.receivedOn between :start and :end
        """)
    BigDecimal sumByUserAndPeriod(User user, LocalDate start, LocalDate end);
}
