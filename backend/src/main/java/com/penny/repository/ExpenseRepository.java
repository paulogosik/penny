package com.penny.repository;

import com.penny.model.CreditCard;
import com.penny.model.Expense;
import com.penny.model.ExpenseCategory;
import com.penny.model.InstallmentPlan;
import com.penny.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ExpenseRepository extends JpaRepository<Expense, UUID>, JpaSpecificationExecutor<Expense> {

    @Override
    @EntityGraph(attributePaths = {"category", "creditCard", "installmentPlan"})
    Page<Expense> findAll(Specification<Expense> specification, Pageable pageable);

    @EntityGraph(attributePaths = {"category", "creditCard", "installmentPlan"})
    Optional<Expense> findByIdAndUser(UUID id, User user);

    @EntityGraph(attributePaths = {"category", "creditCard", "installmentPlan"})
    List<Expense> findByInstallmentPlanOrderByInstallmentNumber(InstallmentPlan installmentPlan);

    @EntityGraph(attributePaths = {"category", "creditCard", "installmentPlan"})
    List<Expense> findByInstallmentPlanInOrderByInstallmentNumber(Collection<InstallmentPlan> installmentPlans);

    @EntityGraph(attributePaths = {"category", "creditCard", "installmentPlan"})
    List<Expense> findByUserAndCreditCardAndBillingMonthOrderByOccurredOnAsc(
        User user,
        CreditCard creditCard,
        LocalDate billingMonth
    );

    boolean existsByCategory(ExpenseCategory category);

    boolean existsByCreditCard(CreditCard creditCard);

    @Query("""
        select coalesce(sum(e.amount), 0)
        from Expense e
        where e.user = :user and e.occurredOn between :start and :end
        """)
    BigDecimal sumByUserAndPeriod(User user, LocalDate start, LocalDate end);

    @Query("""
        select new com.penny.repository.AmountByKey(e.category.id, sum(e.amount))
        from Expense e
        where e.user = :user and e.occurredOn between :start and :end
        group by e.category.id
        """)
    List<AmountByKey> sumByCategory(User user, LocalDate start, LocalDate end);

    @Query("""
        select new com.penny.repository.AmountByKey(e.creditCard.id, sum(e.amount))
        from Expense e
        where e.user = :user and e.billingMonth = :billingMonth
        group by e.creditCard.id
        """)
    List<AmountByKey> sumByCreditCard(User user, LocalDate billingMonth);
}
