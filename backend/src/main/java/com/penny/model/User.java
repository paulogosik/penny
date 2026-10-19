package com.penny.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.time.ZoneId;
import java.util.UUID;

@Entity
@Table(name = "users")
public class User {

    public static final String DEFAULT_TIMEZONE = "America/Sao_Paulo";

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private Long telegramUserId;

    private String name;

    private String timezone;

    private String phoneNumber;

    @CreationTimestamp
    private Instant createdAt;

    protected User() {
    }

    public User(Long telegramUserId, String name, String timezone) {
        this.telegramUserId = telegramUserId;
        this.name = name;
        this.timezone = timezone;
    }

    public ZoneId zoneId() {
        return ZoneId.of(timezone);
    }

    public UUID getId() {
        return id;
    }

    public Long getTelegramUserId() {
        return telegramUserId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public boolean hasPhoneNumber() {
        return phoneNumber != null;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
