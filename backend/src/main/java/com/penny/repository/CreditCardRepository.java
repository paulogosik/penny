package com.penny.repository;

import com.penny.model.CreditCard;
import com.penny.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CreditCardRepository extends JpaRepository<CreditCard, UUID> {

    List<CreditCard> findByUserAndArchivedAtIsNullOrderByName(User user);

    List<CreditCard> findByUserOrderByName(User user);

    Optional<CreditCard> findByIdAndUser(UUID id, User user);

    boolean existsByUserAndNameIgnoreCaseAndArchivedAtIsNull(User user, String name);

    boolean existsByUserAndNameIgnoreCaseAndArchivedAtIsNullAndIdNot(User user, String name, UUID id);
}
