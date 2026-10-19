package com.penny.repository;

import com.penny.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByTelegramUserId(Long telegramUserId);

    Optional<User> findByPhoneNumber(String phoneNumber);
}
