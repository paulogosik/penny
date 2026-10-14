package com.penny.dto;

import com.penny.model.User;

import java.time.Instant;
import java.util.UUID;

public record UserResponse(UUID id, Long telegramUserId, String name, String timezone, Instant createdAt) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getTelegramUserId(), user.getName(), user.getTimezone(), user.getCreatedAt());
    }
}
